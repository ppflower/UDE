package ude.binder.anlysis;

import soot.*;
import soot.jimple.*;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.forward.SyncRequestTaintAnalysis;

import java.util.List;

public class ResultPassAnalysis extends SyncRequestTaintAnalysis {
    public ResultPassAnalysis(SootMethod sootMethod, Stmt startStmt, FlowSet<Value> startStmtTaintedValues) {
        super(sootMethod, startStmt, startStmtTaintedValues);
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
//        System.out.println(String.format("%-25s", "ResultPass[" + sootMethod.getName() + "] ") + String.format("%-30s", in.toString()) + unit);
        super.flowThrough(in, unit, out);
    }

    public boolean isReturnValueTainted() {
        List<Unit> returnStmts = graph.getTails();

        for (Unit unit: returnStmts) {
            Stmt stmt = (Stmt) unit;
            if (stmt instanceof ReturnStmt) {
                Value returnValue = ((ReturnStmt) stmt).getOp();
                FlowSet<Value> results = getFlowBefore(stmt);
                if (results.contains(returnValue))
                    return true;
            }
        }
        return false;
    }

    public static boolean passToWrapperResult(SootMethod sootMethod, Stmt startStmt) {
        if (!(sootMethod.getReturnType() instanceof RefType)) {
            return false;
        }

        if (startStmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) startStmt).getLeftOp();
            FlowSet<Value> startStmtTaintedValues = new ArraySparseSet<>();
            startStmtTaintedValues.add(leftOp);

            ResultPassAnalysis analysis = new ResultPassAnalysis(sootMethod, startStmt, startStmtTaintedValues);

        }

        return false;
    }
}

