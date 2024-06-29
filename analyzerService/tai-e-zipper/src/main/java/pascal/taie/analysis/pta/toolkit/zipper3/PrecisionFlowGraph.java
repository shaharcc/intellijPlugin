
package pascal.taie.analysis.pta.toolkit.zipper3;

import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.*;
import pascal.taie.util.graph.Graph;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PrecisionFlowGraph implements Graph<Node> {

    private final Type type;
    private final ObjectFlowGraph ofg;
    private final Set<Node> visitedNodes;
    private final MultiMap<Node, FlowEdge> inWUEdges;
    private final MultiMap<Node, FlowEdge> outWUEdges;
    private final PointerAnalysisResultEx pta;
    private final ObjectAllocationGraph oag;
    private final Set<JMethod> methods;
    private final Set<VarNode> inNodes;

    PrecisionFlowGraph(PointerAnalysisResultEx pta, ObjectFlowGraph ofg, ObjectAllocationGraph oag, Type type) {
        this.pta = pta;
        this.ofg = ofg;
        this.oag = oag;
        this.type = type;
        this.methods = obtainMethods();
        this.inNodes = obtainInNodes();
        this.visitedNodes = new IndexerBitSet<>(ofg, true);
        this.outWUEdges = Maps.newMultiMap();
        this.inWUEdges = Maps.newMultiMap();
        initialize();
    }

    private void initialize() {
        for (VarNode inNode : inNodes) {
            dfs(inNode);
        }
        for (FlowEdge edge : outWUEdges.values()) {
            inWUEdges.put(edge.target(), edge);
        }
    }

    private Set<JMethod> obtainMethods() {
        return pta.getObjectsOf(type).stream()
                .map(pta::getMethodsInvokedOn)
                .flatMap(Set::stream)
                .filter(Predicate.not(JMethod::isPrivate))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<VarNode> obtainInNodes() {
        return methods.stream()
                .flatMap(method -> method.getIR().getParams().stream())
                .filter(param -> !pta.getBase().getPointsToSet(param).isEmpty())
                .map(ofg::getVarNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void dfs(Node startNode) {
        if (visitedNodes.add(startNode)) {
            if (startNode instanceof VarNode varNode) {
                handleVarNode(varNode);
            }
            for (Node targetNode : getAllTargetNodes(startNode)) {
                if (!visitedNodes.contains(targetNode)) {
                    dfs(targetNode);
                }
            }
        }
    }

    private void handleVarNode(VarNode varNode) {
        Var var = varNode.getVar();
        Set<Obj> varPts = pta.getBase().getPointsToSet(var);
        getAssignedVariables(var).forEach(toVar -> {
            VarNode toNode = ofg.getVarNode(toVar);
            if (toNode != null) {
                for (VarNode inNode : inNodes) {
                    Set<Obj> inPts = pta.getBase().getPointsToSet(inNode.getVar());
                    if (!Collections.disjoint(inPts, varPts)) {
                        outWUEdges.put(varNode, new UnwrappedFlowEdge(varNode, toNode));
                    }
                }
            }
        });
    }

    private static List<Var> getAssignedVariables(Var var) {
        return var.getInvokes().stream()
                .map(Invoke::getLValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Node> getAllTargetNodes(Node node) {
        List<Node> nextEdges = new ArrayList<>();
        for (FlowEdge edge : ofg.getOutEdgesOf(node)) {
            Node target = edge.target();
            FlowKind kind = edge.kind();
            if (kind == FlowKind.INSTANCE_STORE || kind == FlowKind.ARRAY_STORE) {
                if (target instanceof InstanceNode toNode) {
                    Obj base = toNode.getBase();
                    if (base.getType().equals(type)) {
                        handleInstanceOrArrayStore(toNode);
                    } else if (oag.getAllocateesOf(type).contains(base)) {
                        handleAllocation(toNode, base);
                    }
                }
            }
            nextEdges.add(target);
        }
        return nextEdges;
    }

    private void handleInstanceOrArrayStore(InstanceNode toNode) {
        pta.getObjectsOf(type).stream()
                .map(pta::getMethodsInvokedOn)
                .flatMap(Set::stream)
                .map(m -> m.getIR().getThis())
                .map(ofg::getVarNode)
                .filter(Objects::nonNull)
                .forEach(nextNode -> outWUEdges.put(toNode, new WrappedFlowEdge(toNode, nextNode)));
    }

    private void handleAllocation(InstanceNode toNode, Obj base) {
        VarNode assignedNode = getAssignedNode(base);
        if (assignedNode != null) {
            outWUEdges.put(toNode, new WrappedFlowEdge(toNode, assignedNode));
        }
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return getInEdgesOf(node).stream()
                .map(FlowEdge::source)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<FlowEdge> getInEdgesOf(Node node) {
        Set<FlowEdge> inEdges = new HashSet<>(ofg.getInEdgesOf(node));
        inEdges.addAll(inWUEdges.get(node));
        return inEdges.stream()
                .filter(edge -> visitedNodes.contains(edge.source()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return getAllTargetNodes(node).stream()
                .filter(visitedNodes::contains)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Node> getVisitedNodes() {
        return visitedNodes;
    }

    public Set<JMethod> getMethods() {
        return methods;
    }

    @Nullable
    private VarNode getAssignedNode(Obj obj) {
        if (obj.getAllocation() instanceof New newStmt) {
            return ofg.getVarNode(newStmt.getLValue());
        }
        return null;
    }
}
