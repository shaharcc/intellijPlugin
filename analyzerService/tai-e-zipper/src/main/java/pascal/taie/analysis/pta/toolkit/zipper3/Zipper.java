
package pascal.taie.analysis.pta.toolkit.zipper3;

import pascal.taie.analysis.graph.flowgraph.InstanceNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultExImpl;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Zipper {

    private final PointerAnalysisResultEx pta;
    private final ObjectAllocationGraph oag;
    private final ObjectFlowGraph ofg;
    private final Collection<JMethod> pcmCollection;
    private final Map<Obj, Set<JMethod>> invokedMethods;

    public Zipper(PointerAnalysisResult ptaBase) {
        this.pta = new PointerAnalysisResultExImpl(ptaBase, true);
        this.oag = new ObjectAllocationGraph(pta);
        this.ofg = new ObjectFlowGraph(pta);
        this.invokedMethods = ObjectAllocationGraph.computeInvokedMethods(pta);
        this.pcmCollection = Collections.synchronizedList(new ArrayList<>());
    }

    public static Set<JMethod> run(PointerAnalysisResult pta, String arg) {
        return new Zipper(pta).analyze();
    }

    public Set<JMethod> analyze() {
        pta.getObjectTypes().parallelStream().forEach(this::collectPCM);
        return Set.copyOf(pcmCollection);
    }

    private void collectPCM(Type type) {
        PrecisionFlowGraph precisionFlowGraph = new PrecisionFlowGraph(pta, ofg, oag, type);
        Set<VarNode> outputNodes = getOutNodes(precisionFlowGraph, type);
        Set<Node> flowNodes = new HashSet<>();
        Deque<Node> nodeQueue = new ArrayDeque<>();

        for (VarNode outputNode : outputNodes) {
            nodeQueue.add(outputNode);
            while (!nodeQueue.isEmpty()) {
                Node currentNode = nodeQueue.poll();
                if (flowNodes.add(currentNode)) {
                    precisionFlowGraph.getPredsOf(currentNode).stream()
                            .filter(node -> !flowNodes.contains(node))
                            .forEach(nodeQueue::add);
                }
            }
        }

        Set<JMethod> potentialMethods = flowNodes.stream()
                .map(Zipper::node2Method)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (!potentialMethods.isEmpty()) {
            synchronized (pcmCollection) {
                pcmCollection.addAll(potentialMethods);
            }
        }
    }


    private Set<VarNode> getOutNodes(PrecisionFlowGraph pfg, Type type) {
        Set<JMethod> outMethods = new HashSet<>(pfg.getMethods());
        Set<JMethod> typeMethods = Stream
                .concat(pta.getObjectsOf(type).stream(), oag.getAllocateesOf(type).stream())
                .flatMap(obj -> invokedMethods.getOrDefault(obj, Collections.emptySet()).stream())
                .collect(Collectors.toSet());
        typeMethods
                .stream()
                .filter(m -> !m.isPrivate())
                .filter(m -> (m.isStatic() && isSpecialAccessMethod(m, type)) || isInnerClass(m.getDeclaringClass(), type))
                .forEach(outMethods::add);

        return outMethods.stream().flatMap(method -> method.getIR().getReturnVars().stream())
                .filter(retVar -> !pta.getBase().getPointsToSet(retVar).isEmpty())
                .map(ofg::getVarNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isSpecialAccessMethod(JMethod method, Type type) {
        return method.getDeclaringClass().getType().equals(type) && method.getName().startsWith("access$");
    }

    private boolean isInnerClass(JClass jclass, Type type) {
        if (type instanceof ClassType classType) {
            JClass outer = classType.getJClass();
            while (outer != null) {
                for (JClass inner = jclass; inner != null; inner = inner.getOuterClass()) {
                    if (inner.equals(outer) || Objects.equals(inner.getOuterClass(), outer)) {
                        return true;
                    }
                }
                outer = outer.getSuperClass();
            }
        }
        return false;
    }

    @Nullable
    private static JMethod node2Method(Node node) {
        if (node instanceof VarNode varNode) {
            return varNode.getVar().getMethod();
        } else if (node instanceof InstanceNode instanceNode) {
            if (instanceNode.getBase().getAllocation() instanceof New newStmt) {
                return newStmt.getContainer();
            }
        }
        return null;
    }
}
