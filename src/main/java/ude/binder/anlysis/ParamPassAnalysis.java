package ude.binder.anlysis;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.AppAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ParamPassAnalysis extends BackwardBinderAnalysis {
    SootMethod sm;
    Unit taintUnit;
    FlowSet<Value> initialTaintedValues;

    boolean isSelfObjectTainted;
    boolean isRetValTainted;

    MustAliasAnalysis mustAliasAnalysis;

    HashSet<Object> importantInformation;


    public static HashSet<SootMethod> inAnalysisSet;
    public static FlowSet<SootMethod> filteredMethodList;
    public static FlowSet<SootClass> filteredMethodClassList;

    public static void initConsts() {
        inAnalysisSet = new HashSet<>();

        filteredMethodList = new ArraySparseSet<>();
        filteredMethodList.add(Scene.v().getMethod("<java.lang.StringBuilder: java.lang.String toString()>"));
        filteredMethodList.add(Scene.v().getMethod("<java.lang.Object: java.lang.String toString()>"));
        filteredMethodList.add(Scene.v().getMethod("<java.lang.String: byte[] getBytes()>"));
        filteredMethodList.add(Scene.v().getMethod("<java.lang.String: java.lang.String replace(java.lang.CharSequence,java.lang.CharSequence)>"));
        filteredMethodList.add(Scene.v().getMethod("<java.lang.String: byte[] getBytes(java.lang.String)>"));
        filteredMethodList.add(Scene.v().getMethod("<java.util.List: int size()>"));

        filteredMethodClassList = new ArraySparseSet<>();
        filteredMethodClassList.add(Scene.v().getSootClass("java.util.List"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.lang.Object"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.util.Set"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.util.Map"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.util.HashMap"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.util.Iterator"));
        filteredMethodClassList.add(Scene.v().getSootClass("android.content.Context"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.lang.CharSequence"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.lang.StringBuilder"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.lang.StringBuffer"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.lang.String"));
        filteredMethodClassList.add(Scene.v().getSootClass("org.json.JSONObject"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.io.InputStream"));
        filteredMethodClassList.add(Scene.v().getSootClass("java.io.OutputStream"));
    }

    public ParamPassAnalysis(SootMethod sm, Unit taintUnit, FlowSet<Value> initialTaintedValues, boolean isSelfObjectTainted, boolean isRetValTainted) {
        super(new BriefUnitGraph(sm.retrieveActiveBody()));
        this.sm = sm;
        this.taintUnit = taintUnit;
        this.initialTaintedValues = initialTaintedValues;
        this.isSelfObjectTainted = isSelfObjectTainted;
        this.isRetValTainted = isRetValTainted;

//        if (!AppAnalyzer.analyzedAliases.containsKey(sm)) {
//            AppAnalyzer.analyzedAliases.put(sm, new MustAliasAnalysis(sm));
//        }
//        this.mustAliasAnalysis = AppAnalyzer.analyzedAliases.get(sm);
        this.mustAliasAnalysis = new MustAliasAnalysis(sm);

        this.importantInformation = new HashSet<>();

        doAnalysis();
//        System.out.println();
    }

    @Override
    public void doAnalysis() {
        inAnalysisSet.add(sm);
//        System.out.println("ParamPass Analyzing " + sm);
        super.doAnalysis();
        inAnalysisSet.remove(sm);;
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
        Stmt stmt = (Stmt) unit;

//        if (sm.toString().equals("<com.analysisplus.app.graph.Graph: void Unfollow(java.lang.String,com.analysisplus.app.graph.Graph$ResponseHandler)>")) {
//            System.out.println(String.format("%-15s", "ParamPass[" + sm.getName() + "] ") + String.format("%-30s", in.toString()) + unit);
//        }
//        else
//        if (sm.toString().equals("<com.netease.nim.uikit.business.session.fragment.MessageFragment: void requestGiftAPI(java.lang.String,int,int,java.lang.String,boolean)>")) {
//            System.out.println(String.format("%-25s", "ParamPass[" + sm.getName() + "] ") + String.format("%-30s", in.toString()) + unit);
//        }

        in.copy(out);
        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();
        if (stmt == taintUnit) {
            genSet.union(initialTaintedValues);
        }
        else if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            Value rightOp = ((AssignStmt) stmt).getRightOp();
            if (in.contains(leftOp)) {
                killSet.add(leftOp); // todo 可能要按照类型分情况讨论
                if (rightOp instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                    SootMethod tgtMethod = invokeExpr.getMethod();
                    if (!tgtMethod.getDeclaringClass().isApplicationClass() || AppAnalyzer.is3rdPartyLibrary(tgtMethod.getDeclaringClass())) {
                        if (invokeExpr instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                            Value invokeObject = instanceInvokeExpr.getBase();
                            genSet.add(invokeObject);
                        }
                        for (Value arg: invokeExpr.getArgs()) {
                            if (arg instanceof Local) genSet.add(arg);
                        }
                        recordImportantInformation(stmt);
                    }
                    else {
                        List<SootMethod> tgtMethods = parseMethods(unit);
                        if (tgtMethods.size() > 1) {
                            // 假设call graph构建的边是准确的
                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                                Value invokeObject = instanceInvokeExpr.getBase();
                                genSet.add(invokeObject);
                            }
                            else {
                                for (Value arg: invokeExpr.getArgs()) {
                                    if (arg instanceof Local) genSet.add(arg);
                                }
                            }
                        }
                        else {
                            SootMethod singleTgt = stmt.getInvokeExpr().getMethod();
                            if (tgtMethods.size() == 1) {
                                singleTgt = tgtMethods.get(0);
                            }
                            if (!singleTgt.isAbstract() && !singleTgt.isNative() && !singleTgt.isPhantom()) {
                                boolean resTainted = true;
                                if (!inAnalysisSet.contains(singleTgt)) {
                                    boolean invokeObjectTainted = false;
                                    if (invokeExpr instanceof InstanceInvokeExpr) {
                                        InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                                        Value invokeObject = instanceInvokeExpr.getBase();
                                        if (in.contains(invokeObject)) {
                                            invokeObjectTainted = true;
                                        }
                                    }
                                    ParamPassAnalysis paramPassAnalysis = new ParamPassAnalysis(singleTgt, null, null, invokeObjectTainted, resTainted);
                                    List<ParameterRef> taintedParams = paramPassAnalysis.getTaintedParams();
                                    for (ParameterRef parameterRef: taintedParams) {
                                        int taintedIndex = parameterRef.getIndex();
                                        Value arg = invokeExpr.getArg(taintedIndex);
                                        if (arg instanceof Local) genSet.add(arg);
                                    }
                                    if (invokeExpr instanceof InstanceInvokeExpr) {
                                        if (!invokeObjectTainted) {
                                            if (paramPassAnalysis.isInvokeObjectTainted()) {
                                                genSet.add(((InstanceInvokeExpr) invokeExpr).getBase());
                                            }
                                        }
                                    }
                                    this.importantInformation.addAll(paramPassAnalysis.getImportantInformation());
                                }
                            }
                        }
                    }
                }
                else if (rightOp instanceof Local) {
                    genSet.add(rightOp);
                }
                else if (rightOp instanceof InstanceFieldRef) {
                    // todo   val = thisObj.field, val = obj.field
                    Value fieldBase = ((InstanceFieldRef) rightOp).getBase();
//                    if (!sm.isStatic() && fieldBase != sm.retrieveActiveBody().getThisLocal()) { // 不用这个限制条件了，因为看到了一个例子，调用对象存着request的内容
                    genSet.add(fieldBase);
                }
                else if (rightOp instanceof ArrayRef) {
                    Value arrayBase = ((ArrayRef) rightOp).getBase();
                    Value index = ((ArrayRef) rightOp).getIndex();
                    if (arrayBase instanceof Local) genSet.add(arrayBase);
                    if (index instanceof Local) genSet.add(index);
                }
                else if (rightOp instanceof CastExpr) {
                    genSet.add(((CastExpr) rightOp).getOp());
                }
                else if (rightOp instanceof BinopExpr) {
                    Value op1 = ((BinopExpr) rightOp).getOp1();
                    if (op1 instanceof Local) genSet.add(op1);
                    Value op2 = ((BinopExpr) rightOp).getOp2();
                    if (op2 instanceof Local) genSet.add(op2);
                }
                else if (rightOp instanceof NegExpr) {
                    Value op = ((NegExpr) rightOp).getOp();
                    genSet.add(op);
                }
            }
            // todo 其他情况，thisObj.field = val
            else if (leftOp instanceof InstanceFieldRef) {
                Value fieldBase = ((InstanceFieldRef) leftOp).getBase();
                if (in.contains(fieldBase)) {
                    if (rightOp instanceof Local)
                        genSet.add(rightOp);
                }
            }
            else if (leftOp instanceof ArrayRef) {
                Value arrayBase = ((ArrayRef) leftOp).getBase();
                if (in.contains(arrayBase)) {
                    if (rightOp instanceof Local)
                        genSet.add(rightOp);
                }
            }
        }
        else if (stmt.containsInvokeExpr()) {
            // obj.func(f1, f2)
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr.getArgCount() > 0) {
                if (invokeExpr instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                    Value invokeObject = instanceInvokeExpr.getBase();
                    if (in.contains(invokeObject)) {
                        SootMethod tgtMethod = invokeExpr.getMethod();
                        if (!tgtMethod.getDeclaringClass().isApplicationClass() || AppAnalyzer.is3rdPartyLibrary(tgtMethod.getDeclaringClass())) {
                            for (Value arg: invokeExpr.getArgs()) {
                                if (arg instanceof Local) genSet.add(arg);
                            }
                            recordImportantInformation(stmt);
                        }
                        else {
                            List<SootMethod> tgtMethods = parseMethods(unit);
                            if (tgtMethods.size() > 1) {
                                // 假设call graph构建的边是准确的
                                for (Value arg: invokeExpr.getArgs()) {
                                    if (arg instanceof Local) genSet.add(arg);
                                }
                            }
                            else {
                                SootMethod singleTgt = stmt.getInvokeExpr().getMethod();
                                if (tgtMethods.size() == 1) {
                                    singleTgt = tgtMethods.get(0);
                                }
                                if (!singleTgt.isAbstract() && !singleTgt.isNative() && !singleTgt.isPhantom()) {
                                    if (!inAnalysisSet.contains(singleTgt)) {
                                        boolean resTainted = false;
                                        boolean invokeObjectTainted = true;
                                        ParamPassAnalysis paramPassAnalysis = new ParamPassAnalysis(singleTgt, null, null, invokeObjectTainted, resTainted);
                                        List<ParameterRef> taintedParams = paramPassAnalysis.getTaintedParams();
                                        for (ParameterRef parameterRef: taintedParams) {
                                            int taintedIndex = parameterRef.getIndex();
                                            Value arg = invokeExpr.getArg(taintedIndex);
                                            if (arg instanceof Local) genSet.add(arg);
                                        }
                                        this.importantInformation.addAll(paramPassAnalysis.getImportantInformation());
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    //todo record important info
                    SootMethod tgtMethod = invokeExpr.getMethod();
                    SootClass declaringClass = tgtMethod.getDeclaringClass();
                    if (declaringClass.isApplicationClass()) {
                        FlowSet<Integer> candidateParamIndexes = new ArraySparseSet<>();
                        for (int i = 0; i < tgtMethod.getParameterCount(); i ++) {
                            Value arg = invokeExpr.getArg(i);
                            if (in.contains(arg)) {
                                Type argType = arg.getType();
                                if (argType instanceof RefType) {
                                    SootClass argClass = ((RefType) argType).getSootClass();
//                                    if (argClass.isLibraryClass() || AppAnalyzer.is3rdPartyLibrary(argClass)) {
                                    if (argClass==Scene.v().getSootClass("com.loopj.android.http.RequestParams")) {
                                        candidateParamIndexes.add(i);
                                    }
                                }
                            }
                        }
                        if (!candidateParamIndexes.isEmpty()) {
                            if (!ParamTaintAnalysis.inAnalysisSet.contains(tgtMethod)) {
                                ParamTaintAnalysis paramTaintAnalysis = new ParamTaintAnalysis(tgtMethod, candidateParamIndexes);
                                HashSet<Object> importantInfo = paramTaintAnalysis.getImportantInformation();
                                this.importantInformation.addAll(importantInfo);
                            }
                        }
                        // todo results
                    }
                }
            }
        } else if (isRetValTainted) {
            if (stmt instanceof ReturnStmt) {
                Value returnValue = ((ReturnStmt) stmt).getOp();
                if (returnValue instanceof Local) genSet.add(returnValue);
            }
        }

        out.difference(killSet);
        out.union(genSet);
        for (Value value: out) {
            FlowSet<Value> aliases = getAliasSet(unit, value);
            out.union(aliases);
        }
    }

    public FlowSet<Value> getAliasSet(Unit unit, Value value) {
        HashMap<Unit, HashSet<FlowSet<Value>>> aliases =  this.mustAliasAnalysis.getAliases();
        HashSet<FlowSet<Value>> aliasesBeforeUnit = aliases.get(unit);
        for (FlowSet<Value> candidateSet: aliasesBeforeUnit) {
            if (candidateSet.contains(value)) {
                return candidateSet;
            }
        }
        FlowSet<Value> res = new ArraySparseSet<>();
        res.add(value);
        return res;
    }

    protected FlowSet<Value> entryInitialFlow() {
        FlowSet<Value> res = new ArraySparseSet<>();
        if (isSelfObjectTainted) {
            res.add(sm.retrieveActiveBody().getThisLocal());
        }
        return res;
    }

    public boolean isInvokeObjectTainted() {
        if (sm.isStatic()) {
            return false;
        }
        Value thisObj = this.sm.retrieveActiveBody().getThisLocal();
        FlowSet<Value> allTaintedValues = new ArraySparseSet<>();
        List<Unit> heads = graph.getHeads();
        for (Unit unit: heads) {
            FlowSet<Value> taintedValues = getFlowBefore(unit);
            allTaintedValues.union(taintedValues);
        }
        return allTaintedValues.contains(thisObj);
    }
    public List<ParameterRef> getTaintedParams() {
        List<ParameterRef> res = new ArrayList<>();

        FlowSet<Value> allTaintedValues = new ArraySparseSet<>();
        List<Unit> heads = graph.getHeads();
        for (Unit unit: heads) {
            FlowSet<Value> taintedValues = getFlowBefore(unit);
            allTaintedValues.union(taintedValues);
        }

        Body body = sm.retrieveActiveBody();
        List<Value> params = body.getParameterRefs();
        for (Value param: params) {
            ParameterRef parameterRef = (ParameterRef) param;
            Local local = body.getParameterLocal(parameterRef.getIndex());
            if (allTaintedValues.contains(local)) {
                res.add(parameterRef);
            }
        }
        return res;
    }

    public ParameterRef getSingleTaintedParam(SootClass expectedTypeClass) {
        ParameterRef res = null;
        int resCount = 0;
        FlowSet<Value> taintedValues = new ArraySparseSet<>();
        for (Unit unit: this.graph.getHeads()) {
            taintedValues.union(getFlowBefore(unit));
        }
        Body body = this.sm.retrieveActiveBody();
        List<Value> paramRefs = body.getParameterRefs();
        for (Value value: paramRefs) {
            ParameterRef paramRef = (ParameterRef) value;
            Value paramValue = body.getParameterLocal(paramRef.getIndex());
            if (taintedValues.contains(paramValue)) {
                Type type = paramValue.getType();
                if (type instanceof RefType) {
                    SootClass paramClass = ((RefType) paramValue.getType()).getSootClass();
                    Hierarchy hierarchy = Scene.v().getActiveHierarchy();
                    if (expectedTypeClass.isInterface()) {
                        if (paramClass.isInterface()) {
                            if (hierarchy.isInterfaceSubinterfaceOf(paramClass, expectedTypeClass)) {
                                res = paramRef;
                                resCount ++;
                            }
                        }
                        else {
                            if (hierarchy.getImplementersOf(expectedTypeClass).contains(paramClass)) {
                                res = paramRef;
                                resCount ++;
                            }
                        }
                    }
                    else if (!paramClass.isInterface() && !paramClass.isPhantom()) {
                        if (hierarchy.getSuperclassesOfIncluding(paramClass).contains(expectedTypeClass)) {
                            // 这里不能用isClassSubclassOfIncluding()方法，因为对于一些常见Library class，Hierarchy并不能给出正确的关系。
                            res = paramRef;
                            resCount ++;
                        }
                    }
                }
            }
        }
        if (resCount > 1) {
            System.out.println("!!!!Find more related params in " + sm);
        }
        return res;
    }

    public List<SootMethod> parseMethods(Unit unit) {
        CallGraph callGraph = Scene.v().getCallGraph();
        Targets targets = new Targets(callGraph.edgesOutOf(unit));
        List<SootMethod> res = new ArrayList<>();
        Stmt stmt = (Stmt) unit;
        String methodName = stmt.getInvokeExpr().getMethod().getName();
        while (targets.hasNext()) {
            SootMethod sootMethod = (SootMethod) targets.next();
            if (sootMethod.getName().equals(methodName)) res.add(sootMethod);
        }
        return res;
    }



    public void recordImportantInformation(Stmt stmt) {
        if (stmt instanceof AssignStmt) {
            // 记录系统函数结果
            SootMethod tgtMethod = stmt.getInvokeExpr().getMethod();
            if (!filteredMethodClassList.contains(tgtMethod.getDeclaringClass()) && !filteredMethodList.contains(tgtMethod)) {
                this.importantInformation.add(tgtMethod);
            }
        }
        else {
            // 记录KV结构put函数的key
            InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
            SootMethod sootMethod = invokeExpr.getMethod();
            if (sootMethod.getParameterCount() == 2) {
                if (sootMethod.getName().startsWith("put")) {
                    Value firstArg = invokeExpr.getArg(0);
                    if (firstArg instanceof StringConstant) {
                        this.importantInformation.add("KeyString:" + ((StringConstant) firstArg).value);
                    }
                }
            }
        }
    }

    public HashSet<Object> getImportantInformation() {
        return this.importantInformation;
    }

}
