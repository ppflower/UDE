package ude.forward;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.toolkits.scalar.FlowSet;

public class SyncRequestTaintAnalysis extends ForwardTaintAnalysis {

    protected Stmt startStmt;
    protected FlowSet<Value> startStmtTaintedValues;

    public SyncRequestTaintAnalysis(SootMethod sootMethod, Stmt startStmt, FlowSet<Value> startStmtTaintedValues) {
        super(sootMethod);
        this.startStmt = startStmt;
        this.startStmtTaintedValues = startStmtTaintedValues;

        doAnalysis();

    }


    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
        Stmt stmt = (Stmt) unit;
        if (stmt == this.startStmt){

            if (isDebugging) {
                System.out.println(String.format("%-35s", "[" + sootMethod.getDeclaringClass().getShortName() + " " + sootMethod.getName() + "] ") + String.format("%-30s", in.toString()) + stmt);
            }

            // todo
            in.copy(out);
            out.union(startStmtTaintedValues);
        } else {
            super.flowThrough(in, stmt, out);
        }
    }


}
