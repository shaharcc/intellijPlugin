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

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.NodeManager;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.PointerFlowGraph;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;
import java.util.Set;

public class ObjectFlowGraph extends NodeManager implements Graph<Node>, Indexer<Node> {

    private final MultiMap<Node, FlowEdge> inEdges = Maps.newMultiMap(new IndexMap<>(this, 4096));
    private final MultiMap<Node, FlowEdge> outEdges = Maps.newMultiMap(new IndexMap<>(this, 4096));

    public ObjectFlowGraph(PointerAnalysisResultEx pta) {
        PointerFlowGraph pfg = pta.getBase().getPointerFlowGraph();
        CallGraph<Invoke, JMethod> callGraph = pta.getBase().getCallGraph();
        pfg.pointers().forEach(pointer -> {
            toNode(pointer);
            pfg.getOutEdgesOf(pointer).forEach(this::addPointerFlowEdge);
        });
        callGraph.edges().forEach(e -> {
            if (e.getKind() != CallKind.OTHER &&
                    e.getCallSite().getInvokeExp() instanceof InvokeInstanceExp invokeExp) {
                addFlowEdge(new BasicFlowEdge(FlowKind.THIS_PASSING,
                        getOrCreateVarNode(invokeExp.getBase()),
                        getOrCreateVarNode(e.getCallee().getIR().getThis())));
            }
        });
    }

    private void addPointerFlowEdge(PointerFlowEdge edge) {
        FlowEdge flowEdge = edge.kind() == FlowKind.OTHER ?
                new OtherFlowEdge(edge.getInfo(), toNode(edge.source()), toNode(edge.target())) :
                new BasicFlowEdge(edge.kind(), toNode(edge.source()), toNode(edge.target()));
        addFlowEdge(flowEdge);
    }

    private void addFlowEdge(FlowEdge flowEdge) {
        outEdges.put(flowEdge.source(), flowEdge);
        inEdges.put(flowEdge.target(), flowEdge);
    }

    private Node toNode(Pointer pointer) {
        if (pointer instanceof CSVar csVar) {
            return getOrCreateVarNode(csVar.getVar());
        } else if (pointer instanceof InstanceField iField) {
            return getOrCreateInstanceFieldNode(iField.getBase().getObject(), iField.getField());
        } else if (pointer instanceof ArrayIndex arrayIndex) {
            return getOrCreateArrayIndexNode(arrayIndex.getArray().getObject());
        } else {
            return getOrCreateStaticFieldNode(((StaticField) pointer).getField());
        }
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), FlowEdge::source);
    }

    @Override
    public Set<FlowEdge> getInEdgesOf(Node node) {
        return inEdges.get(node);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), FlowEdge::target);
    }

    @Override
    public Set<FlowEdge> getOutEdgesOf(Node node) {
        return outEdges.get(node);
    }
}
