package ude.binder.anlysis;

import soot.*;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MustAliasAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Value>> {

    private HashMap<Unit, HashSet<FlowSet<Value>>> aliasesAfterUnit;

    private HashMap<Unit, HashSet<FlowSet<Value>>> aliasesBeforeUnit;

    public MustAliasAnalysis(SootMethod sm) {
        super(new BriefUnitGraph(sm.retrieveActiveBody()));

        this.aliasesAfterUnit = new HashMap<>();
        for (Unit unit: sm.retrieveActiveBody().getUnits()) {
            HashSet<FlowSet<Value>> hashSet = new HashSet<>();
            this.aliasesAfterUnit.put(unit, hashSet);
        }

        doAnalysis();


    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {

        HashSet<FlowSet<Value>> mergedPredAliasSets = new HashSet<>();
        List<Unit> preds = this.graph.getPredsOf(unit);
        if (preds.size() > 0) {
            Unit pred0 = preds.get(0);
            HashSet<FlowSet<Value>> predAliasSets = this.aliasesAfterUnit.get(pred0);
            for (FlowSet<Value> aliasSet : predAliasSets) {
                int count = 1;
                for (int i = 1; i < preds.size(); i++) {
                    Unit predi = preds.get(i);
                    HashSet<FlowSet<Value>> predAliasSetsI = this.aliasesAfterUnit.get(predi);
                    for (FlowSet<Value> aliasSetI : predAliasSetsI) {
                        if (aliasSet.equals(aliasSetI)) {
                            count += 1;
                            break;
                        }
                    }
                }
                if (count == preds.size()) {
                    mergedPredAliasSets.add(aliasSet.clone());
                }
            }
        }

        // out里存的是到当前语句为止存在别名的变量
        if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();

            if (leftOp instanceof Local) {
                // todo kill
                if (leftOp.getType() instanceof RefType) {
                    for (FlowSet<Value> aliasSet: mergedPredAliasSets) {
                        if (aliasSet.contains(leftOp)) {
                            aliasSet.remove(leftOp);
                        }
                    }
                }

                if (rightOp instanceof Local) {
                    if (rightOp.getType() instanceof RefType) {
                        // todo 新增别名
                        boolean flag = false;
                        for (FlowSet<Value> aliasSet: mergedPredAliasSets) {
                            if (aliasSet.contains(rightOp)) {
                                aliasSet.add(leftOp);
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            FlowSet<Value> aliasSet = new ArraySparseSet<>();
                            aliasSet.add(leftOp);
                            aliasSet.add(rightOp);
                            mergedPredAliasSets.add(aliasSet);
                        }
                    }
                }
            }
        }

        // todo 删掉没有别名的记录
        HashSet<FlowSet<Value>> newAliasSets = new HashSet<>();
        for (FlowSet<Value> aliasSet: mergedPredAliasSets) {
            if (aliasSet.size() > 1) {
                newAliasSets.add(aliasSet);
            }
            out.union(aliasSet);
        }
        this.aliasesAfterUnit.put(unit, newAliasSets);
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {
        in1.intersection(in2, out);
    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
        source.copy(dest);
    }

    public HashMap<Unit, HashSet<FlowSet<Value>>> getAliases() {

        if (aliasesBeforeUnit == null) {
            this.aliasesBeforeUnit = new HashMap<>();

            Iterator<Unit> unitIterator = graph.iterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();
                HashSet<FlowSet<Value>> mergedPredAliasSets = new HashSet<>();
                List<Unit> preds = this.graph.getPredsOf(unit);
                if (preds.size() > 0) {
                    Unit pred0 = preds.get(0);
                    HashSet<FlowSet<Value>> predAliasSets = this.aliasesAfterUnit.get(pred0);
                    for (FlowSet<Value> aliasSet : predAliasSets) {
                        int count = 1;
                        for (int i = 1; i < preds.size(); i++) {
                            Unit predi = preds.get(i);
                            HashSet<FlowSet<Value>> predAliasSetsI = this.aliasesAfterUnit.get(predi);
                            for (FlowSet<Value> aliasSetI : predAliasSetsI) {
                                if (aliasSet.equals(aliasSetI)) {
                                    count += 1;
                                    break;
                                }
                            }
                        }
                        if (count == preds.size()) {
                            mergedPredAliasSets.add(aliasSet.clone());
                        }
                    }
                }
                this.aliasesBeforeUnit.put(unit, mergedPredAliasSets);
            }
        }

        return aliasesBeforeUnit;
    }

}