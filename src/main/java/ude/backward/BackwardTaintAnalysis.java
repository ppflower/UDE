package ude.backward;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import ude.FileTool;

import java.util.*;

public class BackwardTaintAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Value>> {
    private final Unit invokeUnit;
    private boolean mark;
    private final FlowSet<Value> sensitiveVals;
    private static final List<String> thirdPartyLibraryPrefixList;
    private SootMethod sootMethod;
    //backward body中所有invokeAnalysis的调用
    private final List<Unit> riskUnits;
    private final List<String> riskInfo;
    private List<String> currentCallChain;
    private List<String> analysisDetails;
    private final FlowSet<Integer> parameterIndices;
    private InvokeAnalysisResult invokeAnalysisResult;
    private HashMap<String, InvokeAnalysisResult> allInvokeAnalysisResults;

    //所有backwardAnalysis之间的调用链
    private List<String> backwardCallChain;

    static {
        thirdPartyLibraryPrefixList = FileTool.readLinesFromFile("apis/libraries.txt");
    }

    //sensitiveArgs是指start中的可疑参数，比如在volley中存在回调函数，那些对象将不会出现在sensitiveArgs
    public BackwardTaintAnalysis(SootMethod sm, Unit start, FlowSet<Value> sensitiveVals, List<String> backwardCallChain, List<BackwardTaintAnalysis> backwardTaintAnalyses) {
        super(new BriefUnitGraph(sm.getActiveBody()));

        this.sootMethod = sm;
        this.invokeUnit = start;
        this.mark = false;
        this.sensitiveVals = sensitiveVals;
        this.riskUnits = new ArrayList<>();
        this.riskInfo = new ArrayList<>();
        this.currentCallChain = new ArrayList<>();
        this.currentCallChain.add(sm.getSignature());
        this.analysisDetails = new ArrayList<>();
        this.parameterIndices = new ArraySparseSet<>();
        this.invokeAnalysisResult = new InvokeAnalysisResult(sm, this.currentCallChain);
        allInvokeAnalysisResults = new HashMap<>();
        doAnalysis();
        this.invokeAnalysisResult.addRiskUnits(this.riskUnits);
        this.invokeAnalysisResult.addRiskInfo(this.riskInfo);
        this.invokeAnalysisResult.addAnalysisDetails(this.analysisDetails);
        this.allInvokeAnalysisResults.put(sm.getSignature(), this.invokeAnalysisResult);
        CallGraph cg = Scene.v().getCallGraph();
        this.backwardCallChain = new ArrayList<>();
        this.backwardCallChain.add(sm.getSignature());
        this.backwardCallChain.addAll(backwardCallChain);
        backwardTaintAnalyses.add(this);
        //indices.size > 0 意味着函数参数被taint过，需要程序间进行分析
        if (parameterIndices.size() > 0 && (!existBackwardRecursion(backwardCallChain))) {
            Iterator<Edge> intoEdges = cg.edgesInto(sm);
            while (intoEdges.hasNext()) {
                Edge e = intoEdges.next();
                Stmt stmt = e.srcStmt();

                if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                    if (instanceInvokeExpr.getBase().getType() == sm.getDeclaringClass().getType()) {
                        Unit srcUnit = e.srcUnit();
                        SootMethod newSrcMethod = e.src();
                        //如果存在第三方库调用
                        if (isThirdPartyLibrary(newSrcMethod.getDeclaringClass())) {
                            continue;
                        }
                        FlowSet<Value> vals = new ArraySparseSet<>();
                        InvokeExpr invokeExpr = ((Stmt) srcUnit).getInvokeExpr();
                        for (Integer i : parameterIndices) {
                            Value arg = invokeExpr.getArg(i);
                            vals.add(arg);
                        }
                        new BackwardTaintAnalysis(newSrcMethod, srcUnit, vals, this.backwardCallChain, backwardTaintAnalyses);
                    }
                } else {
                    Unit srcUnit = e.srcUnit();
                    SootMethod newSrcMethod = e.src();
                    //如果存在第三方库调用
                    if (isThirdPartyLibrary(newSrcMethod.getDeclaringClass())) {
                        continue;
                    }
                    FlowSet<Value> vals = new ArraySparseSet<>();
                    InvokeExpr invokeExpr = ((Stmt) srcUnit).getInvokeExpr();
                    for (Integer i : parameterIndices) {
                        Value arg = invokeExpr.getArg(i);
                        vals.add(arg);
                    }
                    new BackwardTaintAnalysis(newSrcMethod, srcUnit, vals, this.backwardCallChain, backwardTaintAnalyses);
                }
            }
        }
    }

    public static void printStmtType(Stmt stmt) {
        if (stmt instanceof NopStmt)
            System.out.println(String.format("%-20s", "NopStmt") + stmt);
        else if (stmt instanceof IdentityStmt)
            System.out.println(String.format("%-20s", "IdentityStmt") + stmt);
        else if (stmt instanceof AssignStmt)
            System.out.println(String.format("%-20s", "AssignStmt") + stmt);
        else if (stmt instanceof IfStmt)
            System.out.println(String.format("%-20s", "IfStmt") + stmt);
        else if (stmt instanceof GotoStmt)
            System.out.println(String.format("%-20s", "GotoStmt") + stmt);
        else if (stmt instanceof TableSwitchStmt)
            System.out.println(String.format("%-20s", "TableSwitchStmt") + stmt);
        else if (stmt instanceof LookupSwitchStmt)
            System.out.println(String.format("%-20s", "LookupSwitchStmt") + stmt);
        else if (stmt instanceof InvokeStmt)
            System.out.println(String.format("%-20s", "InvokeStmt") + stmt);
        else if (stmt instanceof ReturnStmt)
            System.out.println(String.format("%-20s", "ReturnStmt") + stmt);
        else if (stmt instanceof ReturnVoidStmt)
            System.out.println(String.format("%-20s", "ReturnVoidStmt") + stmt);
        else if (stmt instanceof EnterMonitorStmt)
            System.out.println(String.format("%-20s", "EnterMonitorStmt") + stmt);
        else if (stmt instanceof ExitMonitorStmt)
            System.out.println(String.format("%-20s", "ExitMonitorStmt") + stmt);
        else if (stmt instanceof ThrowStmt)
            System.out.println(String.format("%-20s", "ThrowStmt") + stmt);
        else if (stmt instanceof RetStmt)
            System.out.println(String.format("%-20s", "RetStmt") + stmt);
        else
            System.out.println(String.format("%-20s", "NotFoundStmt") + stmt);
    }

    public boolean existBackwardRecursion(List<String> callChain) {
        HashSet<String> tmp = new HashSet<>();
        for (String call : callChain) {
            if (tmp.contains(call))
                return true;
            else
                tmp.add(call);
        }
        return false;
    }

    @Override
    protected void flowThrough(FlowSet<Value> inSet, Unit unit, FlowSet<Value> outSet) {
//        if (this.sootMethod.getSignature().equals("<com.love.club.sv.common.net.b: void a(com.love.club.sv.base.ui.view.i.b,java.lang.String,com.loopj.android.http.RequestParams,com.loopj.android.http.AsyncHttpResponseHandler,boolean)>")) {
//            printStmtType((Stmt) unit);
//        }
        inSet.copy(outSet);
        Stmt stmt = (Stmt) unit;
        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();

        //当前unit == 网络调用Api接口， 将函数中可疑参数加入taintedValues
        if (unit == invokeUnit && stmt.containsInvokeExpr()) {
//            this.riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
            for (Value v : sensitiveVals) {
                if (!(v instanceof StringConstant) && !(v.getType() instanceof NullType))
                    outSet.add(v);
            }
            mark = true;
            this.analysisDetails.add(getGeneralString(inSet.toString()) + getGeneralString(outSet.toString()) + unit);
            return;
        }

        //如果mark = false，意味着尚未到网络库调用语句处
        if (!mark) {
            this.analysisDetails.add(getGeneralString(inSet.toString()) + getGeneralString(outSet.toString()) + unit);
            return;
        }

        //对于使用参数的函数Index进行标记，用于程序间分析
        if (stmt instanceof IdentityStmt && inSet.contains(((IdentityStmt) stmt).getLeftOp())) {
            Value v = ((IdentityStmt) stmt).getRightOp();
            if (v instanceof ParameterRef)
                parameterIndices.add(((ParameterRef) v).getIndex());
            killSet.add(((IdentityStmt) stmt).getLeftOp());
        }

        //赋值语句：左值被taint，kill左值，gen右值
        if (stmt instanceof AssignStmt && isAssignmentTainted(inSet, stmt)) {
            Value lValue = ((AssignStmt) stmt).getLeftOp();
            killSet.add(lValue);

            // 右值分析主要分为以下两部分
            // 1.右值不是调用语句，如果右值是local,genSet加入；如果右值是实例属性，标记为riskUnit
            // 2.右值是调用语句，对于实例调用，如果实例是属于系统类，则直接加入riskUnit
            if (!stmt.containsInvokeExpr()) {
                Value rValue = ((AssignStmt) stmt).getRightOp();
                if (rValue instanceof Local && !(rValue instanceof Constant) && !(rValue.getType() instanceof NullType))
                    genSet.add(rValue);
                if (rValue instanceof CastExpr) {
                    Value tmp = ((CastExpr) rValue).getOp();
                    genSet.add(tmp);
                } else if (rValue instanceof InstanceFieldRef) {
                    riskUnits.add(unit);
                    riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                }
            }
            if (stmt.containsInvokeExpr() && !(stmt.getInvokeExpr() instanceof NewExpr)) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                //如果左值是被taint，那么可以认为右值base也是被taint,并且对于本地变量参数也加入genSet
                for (Value v : invokeExpr.getArgs()) {
                    if (v instanceof Local && !(v instanceof Constant) && !(v.getType() instanceof NullType)) {
                        genSet.add(v);
                    }
                }
                if (invokeExpr instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                    //如果是实例调用，并且调用对象在inSet中，将函数调用的参数generator
                    RefType refType = (RefType) instanceInvokeExpr.getBase().getType();
                    genSet.add(instanceInvokeExpr.getBase());

                    if (refType.getSootClass().isLibraryClass() || isThirdPartyLibrary(instanceInvokeExpr.getMethod().getDeclaringClass())) {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                    } else if (!refType.getSootClass().isJavaLibraryClass() && !refType.getSootClass().isPhantomClass()) {
                        //todo: invokeAnalysis对调用进行分析
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                        SootMethod sm = invokeExpr.getMethod();
                        try {
                            sm.retrieveActiveBody();
                        } catch (Exception e) {
                            return;
                        }
                        if(((InstanceInvokeExpr) invokeExpr).getBase().getType()==sm.getDeclaringClass().getType()){
                            InvokeAnalysisResult invokeAnalysisResult = analyseFunctionCall(sm);
                            this.allInvokeAnalysisResults.put(sm.getSignature(), invokeAnalysisResult);
                        }
                    }
                } else if (invokeExpr instanceof StaticInvokeExpr) {
                    StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) invokeExpr;
                    if (staticInvokeExpr.getMethod().getDeclaringClass().isLibraryClass() || isThirdPartyLibrary(staticInvokeExpr.getMethod().getDeclaringClass())) {
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                    } else {
                        //todo: invokeAnalysis对调用进行分析
                        riskUnits.add(unit);
                        riskInfo.addAll(RiskUnitUtils.extractImportantInfo(unit));
                        SootMethod sm = invokeExpr.getMethod();
                        try {
                            sm.retrieveActiveBody();
                        } catch (Exception e) {
                            return;
                        }
                        InvokeAnalysisResult invokeAnalysisResult = analyseFunctionCall(sm);
                        this.allInvokeAnalysisResults.put(sm.getSignature(), invokeAnalysisResult);
                    }
                }
            }
        } else if (stmt.containsInvokeExpr()) {
            // 该部分没有使用函数返回值的语句，只是做简单标记，如果需要跟踪可能需要points-to analysis,如果base是当前被taint则直接将其加入riskUnit
            // 因为该部分对于kill-gen影响较小，所以这边我只是简单分析实例调用（并且实例是被taint）
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr instanceof InstanceInvokeExpr) {
                Value base = ((InstanceInvokeExpr) invokeExpr).getBase();

                if (inSet.contains(base)) {
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
        this.analysisDetails.add(
                getGeneralString(inSet.toString()) +
                        getGeneralString(outSet.toString()) + unit);
    }

    private static boolean isAssignmentTainted(FlowSet<Value> inSet, Stmt stmt) {
        boolean res = inSet.contains(((AssignStmt) stmt).getLeftOp());
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            for (Value v : invokeExpr.getArgs()) {
                if (inSet.contains(v)) {
                    res = true;
                    break;
                }
            }
        }
        return res;
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

    private String getGeneralString(String str) {
        return String.format("%-30s", str);
    }


    public HashMap<String, InvokeAnalysisResult> getAllInvokeAnalysisResults() {
        return allInvokeAnalysisResults;
    }

    public boolean inSetContainTaintValue(FlowSet<Value> inSet, InvokeExpr invokeExpr) {
        for (Value v : invokeExpr.getArgs()) {
            if (inSet.contains(v))
                return true;
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

    public InvokeAnalysisResult analyseFunctionCall(SootMethod sm) {
        InvokeAnalysis invokeAnalysis = new InvokeAnalysis(sm, this.currentCallChain, this.allInvokeAnalysisResults);
        return invokeAnalysis.getInvokeAnalysisResult();
    }

    public List<String> getBackwardCallChain() {
        return backwardCallChain;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }
}
