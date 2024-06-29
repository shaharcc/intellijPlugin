/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.toolkit.zipper3;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;
import pascal.taie.util.Canonicalizer;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;
import pascal.taie.util.graph.SimpleGraph;
import pascal.taie.util.graph.TopologicalSorter;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Object allocation graph tailored for Zipper.
 */
class ObjectAllocationGraph extends SimpleGraph<Obj> {

    private final Map<Obj, Set<Obj>> obj2Allocatees = Maps.newMap();

    private final Map<Type, Set<Obj>> type2Allocatees = Maps.newConcurrentMap();

    private Indexer<Obj> objIndexer;

    ObjectAllocationGraph(PointerAnalysisResultEx pta) {
        computeInvokedMethods(pta).forEach((obj, methods) -> {
            addNode(obj);
            methods.stream()
                    .map(pta::getObjectsAllocatedIn)
                    .flatMap(Set::stream)
                    .forEach(succ -> {
                        if (!(obj.getType() instanceof ArrayType)) {
                            addEdge(obj, succ);
                        }
                    });
        });
        objIndexer = pta.getBase().getObjectIndexer();
        computeAllocatees(pta);
        objIndexer = null;
        assert getNumberOfNodes() == pta.getBase().getObjects().size();
    }

    Set<Obj> getAllocateesOf(Type type) {
        return type2Allocatees.get(type);
    }

    private Set<Obj> getAllocateesOf(Obj obj) {
        return obj2Allocatees.get(obj);
    }

    private void computeAllocatees(PointerAnalysisResultEx pta) {
        // compute allocatees of objects
        MergedSCCGraph<Obj> mg = new MergedSCCGraph<>(this);
        TopologicalSorter<MergedNode<Obj>> sorter = new TopologicalSorter<>(mg, true);
        Canonicalizer<Set<Obj>> canonicalizer = new Canonicalizer<>();
        sorter.get().forEach(node -> {
            Set<Obj> allocatees = canonicalizer.get(getAllocatees(node, mg));
            node.getNodes().forEach(obj -> obj2Allocatees.put(obj, allocatees));
        });
        // compute allocatees of types
        pta.getObjectTypes().parallelStream().forEach(type -> {
            Set<Obj> allocatees = new IndexerBitSet<>(objIndexer, true);
            pta.getObjectsOf(type)
                    .forEach(o -> allocatees.addAll(getAllocateesOf(o)));
            type2Allocatees.put(type, canonicalizer.get(allocatees));
        });
    }

    private Set<Obj> getAllocatees(
            MergedNode<Obj> node, MergedSCCGraph<Obj> mg) {
        Set<Obj> allocatees = new IndexerBitSet<>(objIndexer, true);
        mg.getSuccsOf(node).forEach(n -> {
            // direct allocatees
            allocatees.addAll(n.getNodes());
            // indirect allocatees
            Obj o = n.getNodes().get(0);
            allocatees.addAll(getAllocateesOf(o));
        });
        Obj obj = node.getNodes().get(0);
        if (node.getNodes().size() > 1 ||
                getSuccsOf(obj).contains(obj)) { // self-loop
            // The merged node is a true SCC
            allocatees.addAll(node.getNodes());
        }
        return allocatees;
    }

    /**
     * Builds object allocation graph.
     *
     * @return the object allocation graph for the program.
     */
    public static Graph<Obj> build(PointerAnalysisResultEx pta) {
        SimpleGraph<Obj> oag = new SimpleGraph<>();
        computeInvokedMethods(pta).forEach((obj, methods) -> methods.stream()
                .map(pta::getObjectsAllocatedIn)
                .flatMap(Set::stream)
                .forEach(succ -> oag.addEdge(obj, succ)));
        return oag;
    }

    /**
     * Computes the methods invoked on all objects in the program.
     * For a static method, say m, we trace back its caller chain until
     * we find the first instance method on the chain, say m' , and
     * consider that m is invoked on the receiver object of m'.
     */
    public static Map<Obj, Set<JMethod>> computeInvokedMethods(
            PointerAnalysisResultEx pta) {
        Map<Obj, Set<JMethod>> invokedMethods = Maps.newConcurrentMap();
        pta.getBase()
                .getObjects()
                .parallelStream()
                .forEach(obj -> {
                    Set<JMethod> methods = Sets.newHybridSet();
                    Queue<JMethod> workList = new ArrayDeque<>(
                            pta.getMethodsInvokedOn(obj));
                    while (!workList.isEmpty()) {
                        JMethod method = workList.poll();
                        methods.add(method);
                        pta.getBase().getCallGraph()
                                .getCalleesOfM(method)
                                .stream()
                                .filter(m -> m.isStatic() && !methods.contains(m))
                                .forEach(workList::add);
                    }
                    invokedMethods.put(obj, methods);
                });
        return invokedMethods;
    }
}
