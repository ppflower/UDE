package ude.forward;

import soot.SootMethod;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

public class MethodAnalysisResult {
//    public static HashMap<SootMethod, HashMap<String, MethodAnalysisResult>> analyzedResults;
    public static MethodAnalysisResult emptyResult;
    static {
//        analyzedResults = new HashMap<>();
        emptyResult = new MethodAnalysisResult(false, new ArraySparseSet<>(), false, null);
    }

    private SootMethod method;

    private boolean isSelfObjectTainted;
    private FlowSet<Integer> taintedParams; // 引用类型的参数可能在函数调用过程中被taint，taintedParams用来记录这些被taint的参数index
    private boolean isResultTainted;

    private TaintPath taintPath;

    public MethodAnalysisResult(boolean isSelfObjectTainted, FlowSet<Integer> taintedParams, boolean isResultTainted, TaintPath taintPath) {
        this.isSelfObjectTainted = isSelfObjectTainted;
        this.taintedParams = taintedParams;
        this.isResultTainted = isResultTainted;
        this.taintPath = taintPath;
    }

    public MethodAnalysisResult(SootMethod sootMethod, boolean isSelfObjectTainted, FlowSet<Integer> taintedParams, boolean isResultTainted, TaintPath taintPath) {
        this.method = sootMethod;
        this.isSelfObjectTainted = isSelfObjectTainted;
        this.taintedParams = taintedParams;
        this.isResultTainted = isResultTainted;
        this.taintPath = taintPath;
    }

    public boolean isSelfObjectTainted() {
        return isSelfObjectTainted;
    }

    public FlowSet<Integer> getTaintedParams() {
        return taintedParams;
    }

    public boolean isResultTainted() {
        return isResultTainted;
    }

    public TaintPath getTaintPath() {
        return taintPath;
    }

    public SootMethod getMethod() {
        return method;
    }

}
