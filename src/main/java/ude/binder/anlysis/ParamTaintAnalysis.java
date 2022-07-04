package ude.binder.anlysis;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.AppAnalyzer;

import java.util.HashSet;
import java.util.List;

/*
* 这个分析主要是用来分析一种特殊情况，即将一个KV对象传入一个函数
* 该函数内对KV对象内传值，但是该函数的结果被丢弃
* */

public class ParamTaintAnalysis extends BackwardBinderAnalysis {

    private FlowSet<Value> initialTaintedValues;
    private HashSet<Object> importantInformation;

    private SootMethod sm;

    public static HashSet<SootMethod> inAnalysisSet;
    public static void initConsts() {
        inAnalysisSet = new HashSet<>();
    }

    public ParamTaintAnalysis(SootMethod sm, FlowSet<Integer> taintedParamIndexes) {
        super(new BriefUnitGraph(sm.retrieveActiveBody()));

        if (taintedParamIndexes != null) {
            this.initialTaintedValues = new ArraySparseSet<>();
            for (Integer i: taintedParamIndexes) {
                this.initialTaintedValues.add(sm.retrieveActiveBody().getParameterLocal(i));
            }
        }

        importantInformation = new HashSet<>();

        this.sm = sm;

        doAnalysis();
    }

    @Override
    public void doAnalysis() {
        inAnalysisSet.add(sm);
        System.out.println("ParamTaint Analyzing " + sm);
        super.doAnalysis();
        inAnalysisSet.remove(sm);;
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
//        System.out.println(String.format("%-25s", "ParamTaint[" + sm.getName() + "] ") + String.format("%-30s", in.toString()) + unit);

        in.copy(out);
        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();

        Stmt stmt = (Stmt) unit;

        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            if (leftOp instanceof Local && in.contains(leftOp)) {
                killSet.add(leftOp);
                Value rightOp = ((AssignStmt) stmt).getRightOp();
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    SootMethod invokedMethod = invokeExpr.getMethod();
                    if (!invokedMethod.getDeclaringClass().isApplicationClass() || AppAnalyzer.is3rdPartyLibrary(invokedMethod.getDeclaringClass())) {
                        if (invokedMethod== Scene.v().getMethod("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>")) {
                            genSet.add(((InstanceInvokeExpr) invokeExpr).getBase());
                            if (invokeExpr.getArg(0) instanceof Local) {
                                genSet.add(invokeExpr.getArg(0));
                            } else if (invokeExpr.getArg(0) instanceof StringConstant) {
                                importantInformation.add("String:" + ((StringConstant) invokeExpr.getArg(0)).value);
                            }
                        }
                        else if (invokedMethod.getSubSignature().equals("java.lang.String toString()")) {
                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                genSet.add(((InstanceInvokeExpr) invokeExpr).getBase());
                            }
                        }
                        else {
                            importantInformation.add(((InvokeExpr) rightOp).getMethod());
                        }
                    }
                    else {
                        if (!ParamPassAnalysis.inAnalysisSet.contains(invokedMethod)) {
                            boolean resTainted = true;
                            boolean invokeObjectTainted = false;
                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                                Value invokeObject = instanceInvokeExpr.getBase();
                                if (in.contains(invokeObject)) {
                                    invokeObjectTainted = true;
                                }
                            }
                            ParamPassAnalysis paramPassAnalysis = new ParamPassAnalysis(invokedMethod, null, null, invokeObjectTainted, resTainted);
                            List<ParameterRef> taintedParams = paramPassAnalysis.getTaintedParams();
                            for (ParameterRef parameterRef: taintedParams) {
                                genSet.add(invokeExpr.getArg(parameterRef.getIndex()));
                            }
                            this.importantInformation.addAll(paramPassAnalysis.getImportantInformation());
                        }
                    }

                }
                else if (rightOp instanceof StringConstant) {
                    importantInformation.add("String:" + ((StringConstant) rightOp).value);
                }
            }
        }
        else if (stmt.containsInvokeExpr()) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr instanceof InstanceInvokeExpr) {
                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                Value base = instanceInvokeExpr.getBase();
                if (instanceInvokeExpr.getMethod().equals(Scene.v().getMethod("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>"))) {
                    if (in.contains(base)) {
                        if (invokeExpr.getArg(0) instanceof Local) {
                            genSet.add(invokeExpr.getArg(0));
                        } else if (invokeExpr.getArg(0) instanceof StringConstant) {
                            importantInformation.add("String:" + ((StringConstant) invokeExpr.getArg(0)).value);
                        }
                    }
                }
                else if (this.initialTaintedValues.contains(base)) {
                    if (instanceInvokeExpr.getMethod().getParameterCount() == 2) {
                        SootMethod sootMethod = instanceInvokeExpr.getMethod();
                        if (sootMethod.getDeclaringClass().isLibraryClass() || AppAnalyzer.is3rdPartyLibrary(sootMethod.getDeclaringClass())) {
                            if (sootMethod.getName().startsWith("put")) {
                                Value firstArg = instanceInvokeExpr.getArg(0);
                                if (firstArg instanceof StringConstant) {
//                                    importantInformation.add("KeyString:" + ((StringConstant) firstArg).value);
                                    importantInformation.add(firstArg);

                                    Value secondArg = instanceInvokeExpr.getArg(1);
                                    if (secondArg instanceof StringConstant) {
                                        importantInformation.add("String:" + ((StringConstant) secondArg).value);
                                    }
                                    else if (secondArg instanceof Local) {
                                        genSet.add(secondArg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // todo 是否要考虑跨函数分析，碰到具体例子再说吧

        out.difference(killSet);
        out.union(genSet);
    }

//    protected FlowSet<Value> entryInitialFlow() {
//        return this.initialTaintedValues;
//    }

    public HashSet<Object> getImportantInformation() {
        return this.importantInformation;
    }
}
