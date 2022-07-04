package ude.backward;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import ude.FileTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

// 该类设计用于解决BackwardTaintAnalysis中的赋值语句中调用问题
// 针对BackwardTaintAnalysis中的调用分析：主要是分为以下两种情况，
// 1.在BackwardTaintAnalysis中函数调用返回值被使用
// 2.在BackwardTaintAnalysis中函数返回值未被使用，这种情况下本次不进行分析，个人认为需要使用到points-to analysis
// 所以该应用主要是针对第一种情况来编写，采用BackwardAnalysis, 标记返回值，如果返回值最终是以instance field或者系统调用结束，则迭代结束，反之，则需要递归进行下一步查找
public class InvokeAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Value>> {
    private List<Unit> riskUnits;

    private List<String> riskInfo;

    private InvokeAnalysisResult invokeAnalysisResult;
    private List<String> currentCallChain;
    private List<String> analysisDetails;
    private HashMap<String, InvokeAnalysisResult> allInvokeAnalysisResults;
    private static final List<String> thirdPartyLibraryPrefixList;

    static {
        thirdPartyLibraryPrefixList = FileTool.readLinesFromFile("apis/libraries.txt");
    }

    public InvokeAnalysis(SootMethod sm, List<String> callChain, HashMap<String, InvokeAnalysisResult> allInvokeAnalysisResults) {
        super(new BriefUnitGraph(sm.retrieveActiveBody()));
        this.riskUnits = new ArrayList<>();
        this.riskInfo = new ArrayList<>();
        this.currentCallChain = new ArrayList<>();
        this.currentCallChain.addAll(callChain);
        this.currentCallChain.add(sm.getSignature());
        this.analysisDetails = new ArrayList<>();
        this.invokeAnalysisResult = new InvokeAnalysisResult(sm, this.currentCallChain);
        this.allInvokeAnalysisResults = allInvokeAnalysisResults;
        doAnalysis();
        invokeAnalysisResult.addRiskUnits(riskUnits);
        invokeAnalysisResult.addRiskInfo(riskInfo);
        invokeAnalysisResult.addAnalysisDetails(this.analysisDetails);
        this.allInvokeAnalysisResults.put(sm.getSignature(), invokeAnalysisResult);
    }

    @Override
    protected void flowThrough(FlowSet<Value> inSet, Unit unit, FlowSet<Value> outSet) {
        inSet.copy(outSet);
        Stmt stmt = (Stmt) unit;

        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();

        if (stmt instanceof ReturnStmt) {
            Value returnVal = ((ReturnStmt) stmt).getOp();
            if ((returnVal instanceof Constant) || (returnVal.getType() instanceof NullType)) {
                riskUnits.add(unit);
                riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
            } else
                genSet.add(returnVal);
        }

        if (stmt instanceof AssignStmt && inSet.contains(((AssignStmt) stmt).getLeftOp())) {
            Value leftVal = ((AssignStmt) stmt).getLeftOp();
            // 左值kill掉
            killSet.add(leftVal);
            // 右值分析主要分为以下两部分
            // 1.右值不是调用语句，如果右值是local,genSet加入；如果右值是实例属性，标记为riskUnit
            // 2.右值是
            if (!stmt.containsInvokeExpr()) {
                Value rightVal = ((AssignStmt) stmt).getRightOp();
                if (rightVal instanceof Local && !(rightVal instanceof Constant) && !(rightVal.getType() instanceof NullType))
                    genSet.add(rightVal);
                if (rightVal instanceof CastExpr) {
                    Value tmp = ((CastExpr) rightVal).getOp();
                    genSet.add(tmp);
                } else if (rightVal instanceof InstanceFieldRef) {
                    riskUnits.add(unit);
                    riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                }
            }
            if (stmt.containsInvokeExpr() && !(stmt.getInvokeExpr() instanceof NewExpr)) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                //如果左值是被taint，那么可以认为右值base也是被taint,并且对于本地变量参数也加入genSet
                for (Value v : invokeExpr.getArgs()) {
                    if (v instanceof Local && !(v instanceof Constant) && !(v.getType() instanceof NullType))
                        genSet.add(v);
                }
                if (invokeExpr instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;

                    RefType refType = (RefType) instanceInvokeExpr.getBase().getType();
                    genSet.add(instanceInvokeExpr.getBase());
                    if (refType.getSootClass().isLibraryClass() || isThirdPartyLibrary(instanceInvokeExpr.getMethod().getDeclaringClass())) {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                    } else if (!refType.getSootClass().isJavaLibraryClass() && !refType.getSootClass().isPhantomClass()) {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                        SootMethod sm = invokeExpr.getMethod();
                        try {
                            sm.retrieveActiveBody();
                        } catch (Exception e) {
                            return;
                        }
                        InvokeAnalysisResult invokeAnalysisResult = analyzeFunctionCall(sm, currentCallChain, this.allInvokeAnalysisResults);
                        this.allInvokeAnalysisResults.put(sm.getSignature(), invokeAnalysisResult);
                    }
                } else if (invokeExpr instanceof StaticInvokeExpr) {
                    StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) invokeExpr;
                    if (staticInvokeExpr.getMethod().getDeclaringClass().isLibraryClass() || isThirdPartyLibrary(staticInvokeExpr.getMethod().getDeclaringClass())) {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                    } else {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                        SootMethod sm = invokeExpr.getMethod();
                        try {
                            sm.retrieveActiveBody();
                        } catch (Exception e) {
                            return;
                        }
                        InvokeAnalysisResult invokeAnalysisResult = analyzeFunctionCall(sm, currentCallChain, this.allInvokeAnalysisResults);
                        this.allInvokeAnalysisResults.put(sm.getSignature(), invokeAnalysisResult);
                    }
                }
            }
        } else if (stmt.containsInvokeExpr()) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr instanceof InstanceInvokeExpr) {
                if (inSet.contains(((InstanceInvokeExpr) invokeExpr).getBase()) || inSetContainTaintValue(inSet, invokeExpr)) {
                    riskUnits.add(unit);
                    riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                    //如果base是被taint,则通过may analysis将函数所有参数也gen
                    for (Value v : invokeExpr.getArgs()) {
                        if (v instanceof Local && !(v instanceof Constant) && !(v.getType() instanceof NullType))
                            genSet.add(v);
                    }
                }
            }
            if (invokeExpr instanceof StaticInvokeExpr) {
                if (inSetContainTaintValue(inSet, invokeExpr)) {
                    riskUnits.add(unit);
                    riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                }
            }
        }
        outSet.difference(killSet);
        outSet.union(genSet);
        this.analysisDetails.add(getGeneralString(inSet.toString()) + getGeneralString(outSet.toString()) + unit);
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
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

    public InvokeAnalysisResult getInvokeAnalysisResult() {
        return this.invokeAnalysisResult;
    }

    //进程递归迭代
    public InvokeAnalysisResult analyzeFunctionCall(SootMethod sm, List<String> callChain, HashMap<String, InvokeAnalysisResult> allInvokeAnalysisResults) {
        if (!existCallRecursion(callChain)) {
            InvokeAnalysis invokeAnalysis = new InvokeAnalysis(sm, callChain, allInvokeAnalysisResults);
            return invokeAnalysis.getInvokeAnalysisResult();
        } else {
            return new InvokeAnalysisResult(sm, callChain);
        }
    }

    public boolean existCallRecursion(List<String> callChain) {
        HashSet<String> tmp = new HashSet<>();
        for (String call : callChain) {
            if (tmp.contains(call))
                return true;
            else
                tmp.add(call);
        }
        return false;
    }

    public boolean isThirdPartyLibrary(SootClass sootClass) {
        String clsName = sootClass.getName();
        for (String thirdPartyLibraryPrefix : thirdPartyLibraryPrefixList) {
            if (clsName.startsWith(thirdPartyLibraryPrefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean inSetContainTaintValue(FlowSet<Value> inSet, InvokeExpr invokeExpr) {
        for (Value v : invokeExpr.getArgs()) {
            if (inSet.contains(v))
                return true;
        }
        return false;
    }

    private String getGeneralString(String str) {
        return String.format("%-30s", str);
    }
}
