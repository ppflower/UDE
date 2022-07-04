package ude.binder.anlysis;

import soot.Unit;
import soot.Value;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public abstract class BackwardBinderAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Value>> {
    public BackwardBinderAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    protected FlowSet<Value> entryInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
        source.copy(dest);
    }
}
