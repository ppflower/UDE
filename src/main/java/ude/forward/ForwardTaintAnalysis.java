package ude.forward;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import ude.AppAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ForwardTaintAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Value>> {
    public static boolean isDebugging = false;

    protected SootMethod sootMethod;
    protected ForwardInvokeContext invokeContext;

    protected HashMap<Value, Value> param2Args;

    protected FlowSet<Value> initialTaintedValues; // 包含被taint的参数，以及被taint的调用对象

    protected TaintPath taintPath = new TaintPath();
    protected IccTransformer iccTransformer = new IccTransformer();

    protected HashSet<Object> importantInformation = new HashSet<>();



    public static HashSet<SootMethod> inAnalysisSet;
    public static FlowSet<SootClass> kvClasses;
    public static SootClass clsGson;

    public static SootMethod mtdThreadStart;
    public static SootMethod mtdRunOnUiThread;
    public static SootClass clsThread;

    public static void initConsts() {
        inAnalysisSet = new HashSet<>();

        kvClasses = new ArraySparseSet<>();
        kvClasses.add(Scene.v().getSootClass("org.json.JSONObject"));
        kvClasses.add(Scene.v().getSootClass("android.os.Bundle"));
        kvClasses.add(Scene.v().getSootClass("android.content.Intent"));

        clsGson = Scene.v().getSootClass("com.google.gson.Gson");

        mtdThreadStart = Scene.v().getMethod("<java.lang.Thread: void start()>");
        mtdRunOnUiThread = Scene.v().getMethod("<android.app.Activity: void runOnUiThread(java.lang.Runnable)>");
        clsThread = Scene.v().getSootClass("java.lang.Thread");
    }
//    static int level = 0;
//    static int TOP_LEVEL = 50; // 限制传递层数

    public ForwardTaintAnalysis(SootMethod sootMethod, ForwardInvokeContext invokeContext) {
        super(new BriefUnitGraph(sootMethod.retrieveActiveBody()));

        if (isDebugging) {
            System.out.println();
            System.out.println("Analyzing method " + sootMethod);
        }

        this.sootMethod = sootMethod;
        this.invokeContext = invokeContext;
        this.param2Args = new HashMap<>();

        if (invokeContext != null) {
            List<Local> paramLocals = sootMethod.getActiveBody().getParameterLocals();
            HashMap<Integer, Value> paramIndex2Arg = invokeContext.getParamIndex2Arg();
            for (Integer index: invokeContext.getParamIndex2Arg().keySet()) {
                param2Args.put(paramLocals.get(index), paramIndex2Arg.get(index));
            }
            if (invokeContext.isCallback()) {
                for (Integer taintedParamIndex: invokeContext.getTaintedParamIndexes()) {
                    Type paramType = paramLocals.get(taintedParamIndex).getType();
                    if (paramType instanceof RefType) {
                        SootClass paramClass = ((RefType) paramType).getSootClass();
                        if (paramClass.isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(paramClass)) {
//                        System.out.println("    ParamType:" + paramClass);
                            importantInformation.add(paramClass);
                        }
                    }
                }
            }
        }

        this.initialTaintedValues = new ArraySparseSet<>();
        if (invokeContext != null) {
//            Body body = sootMethod.retrieveActiveBody();
            Body body = sootMethod.retrieveActiveBody();
            FlowSet<Integer> taintedParamIndexes = invokeContext.getTaintedParamIndexes();
            for (Integer i: taintedParamIndexes) {
                this.initialTaintedValues.add(body.getParameterLocal(i));
            }
            if (!sootMethod.isStatic()) {
                if (invokeContext.isThisObjectTainted()) {
                    this.initialTaintedValues.add(body.getThisLocal());
                }
            }
        }

        doAnalysis();

        if (isDebugging)
            System.out.println();

    }

    protected ForwardTaintAnalysis(SootMethod sootMethod) {
        super(new BriefUnitGraph(sootMethod.retrieveActiveBody()));

        if (isDebugging) {
            System.out.println();
            System.out.println("Analyzing method " + sootMethod);
        }

        this.sootMethod = sootMethod;

        if (isDebugging)
            System.out.println();

    }

    @Override
    public void doAnalysis() {
        inAnalysisSet.add(sootMethod);
//        System.out.println("ForwardTaintAnalysis Analyzing method " + sootMethod);
        super.doAnalysis();
        inAnalysisSet.remove(sootMethod);;
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
        Stmt stmt = (Stmt) unit;

        if (isDebugging) {
            System.out.println(String.format("%-35s", "[" + sootMethod.getDeclaringClass().getShortName() + " " + sootMethod.getName() + "] ") + String.format("%-30s", in.toString()) + stmt);
        }

        in.copy(out);
        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();

        if (stmt instanceof AssignStmt) {
            // Kill the value defined in this unit
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value defValue = assignStmt.getLeftOp();
            if (defValue instanceof Local) {
                killSet.add(defValue);
            }
        }

        // Generate the values tainted in this unit
        MethodAnalysisResult methodAnalysisResult = null;
        if (stmt.containsInvokeExpr()) {
            methodAnalysisResult = analyzeInvoke(in, stmt);
            // 如果说是非静态方法，如果调用对象被taint了，则把它加入到生成集合中
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr instanceof InstanceInvokeExpr) {
                if (methodAnalysisResult.isSelfObjectTainted()) {
                    Value invokeObject = ((InstanceInvokeExpr) invokeExpr).getBase();
                    genSet.add(invokeObject);
                }
            }
            // 函数调用的参数也可能被taint
            for (Integer i: methodAnalysisResult.getTaintedParams()) {
                Value arg = invokeExpr.getArg(i);
                genSet.add(arg);
            }

            recordImportantMethod(invokeExpr.getMethod(), stmt);
        }
        // 如果是赋值语句，如果右侧值被taint了，那么把左侧值加入到生成集合中
        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value defValue = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            if (defValue instanceof InstanceFieldRef) {
                defValue = ((InstanceFieldRef) defValue).getBase();
            } else if (defValue instanceof ArrayRef) {
                defValue = ((ArrayRef) defValue).getBase();
            }
            if (isRightOpTainted(rightOp, methodAnalysisResult, in)) {
                genSet.add(defValue);
                if (rightOp instanceof CastExpr) {
                    recordImportantType(assignStmt);
                }
            }
        }

        out.difference(killSet);
        out.union(genSet);
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected FlowSet<Value> entryInitialFlow() {
//        if (forwardInvokeContext.getTaintedParamIndexes() != null) {
//            return initialTaintedValues.clone();
//        }
//        return super.entryInitialFlow();
        if (initialTaintedValues != null)
            return initialTaintedValues.clone();
        return newInitialFlow();
    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
        source.copy(dest);
    }

    public boolean isRightOpTainted(Value rightOp, MethodAnalysisResult methodAnalysisResult, FlowSet<Value> in) {
        if (rightOp instanceof InvokeExpr) {
            if (methodAnalysisResult.isResultTainted()) {
                return true;
            }
        } else if (rightOp instanceof InstanceFieldRef) {
            Value base = ((InstanceFieldRef) rightOp).getBase();
            return in.contains(base);
        } else if (rightOp instanceof ArrayRef) {
            Value base = ((ArrayRef) rightOp).getBase();
            return in.contains(base);
        } else if (rightOp instanceof Local) {
            return in.contains(rightOp);
        } else if (rightOp instanceof CastExpr) {
            Value op = ((CastExpr) rightOp).getOp();
            return in.contains(op);
        }
        // todo 或许有其他的类型，比如static field ref，后面再补充进来
//        else if (rightOp instanceof Expr) {
//            // 默认是表达式了
//            Expr expr = (Expr) rightOp;
//            for (ValueBox useBox: expr.getUseBoxes()) {
//                Value op = useBox.getValue();
//                if (op instanceof Local) {
//                    if (in.contains(op)) {
//                        return true;
//                    }
//                }
//            }
//        }

        return false;
    }

    public ForwardInvokeContext buildContext(FlowSet<Value> in, Stmt stmt) {
        // ICFG invoke translation
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Value> args = stmt.getInvokeExpr().getArgs();
        boolean isInvokeObjectTainted = false;
        RefLikeType invokeObjectType = null;
        if ((invokeExpr instanceof InstanceInvokeExpr)) {
            Local invokeObject = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
            invokeObjectType = (RefLikeType) invokeObject.getType();
            if (in.contains(invokeObject)) {
                isInvokeObjectTainted = true;
            }
            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                // 获取更加具体的this类型
                if (this.invokeContext != null) {
                    Body body = sootMethod.retrieveActiveBody();
                    if (!sootMethod.isStatic() && invokeObject == body.getThisLocal()) {
                        invokeObjectType = this.invokeContext.getThisObjectType();
                    }
                    else if (body.getParameterLocals().contains(invokeObject)) {
                        List<Local> paramLocals = body.getParameterLocals();
                        int index = paramLocals.indexOf(invokeObject);
                        Value arg = this.invokeContext.getParamIndex2Arg().get(index);
                        if (arg != null) {
                            Type argType = arg.getType();
                            if (argType!=NullType.v()) {
                                invokeObjectType = (RefLikeType) argType;
                            } else {
                                // 如果实例方法的调用对象值为null，就不用分析这句话了
                                // NullType不是RefType
                                return null;
                            }
                        }
                    }
                }
            }
        }

        FlowSet<Integer> taintedParamIndexes = new ArraySparseSet<>();
        HashMap<Integer, Value> paramIndex2Arg = new HashMap<>();
        for (int i = 0; i < args.size(); i ++) {
            Value arg = args.get(i);
            if (in.contains(arg)) {
                taintedParamIndexes.add(i);
            }
            paramIndex2Arg.put(i, arg);
        }

        if (!isInvokeObjectTainted && taintedParamIndexes.isEmpty()) {
            return null;
        }

        ForwardInvokeContext context = new ForwardInvokeContext(isInvokeObjectTainted, invokeObjectType, taintedParamIndexes, paramIndex2Arg);

        // 当判断调用对象为null，或者调用对象未被污染且参数污染为空，返回null
        return context;
    }

    public MethodAnalysisResult analyzeInvoke(FlowSet<Value> in, Stmt stmt) {
        ForwardInvokeContext context = buildContext(in, stmt);
        if (context == null) {
            // 当调用对象为空，或者调用对象未被污染且没有参数被污染，context的值为null，这个时候直接返回一个空结果，不用继续分析
            return MethodAnalysisResult.emptyResult;
        }

        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<SootMethod> possibleMethods = parseMethods(stmt);
        SootMethod tgtMethod = invokeExpr.getMethod();
        MethodAnalysisResult record;
        if (possibleMethods.size() == 1) {
            tgtMethod = possibleMethods.get(0);
        }
        else if (possibleMethods.size() >= 2) {
            // todo 如果存在多条边，需要确认具体是哪个方法
            if (invokeExpr instanceof InstanceInvokeExpr) {
                String tgtSubSignature = tgtMethod.getSubSignature();
                RefLikeType invokeObjectType = context.getThisObjectType();
                if (invokeObjectType instanceof RefType) {
                    SootClass invokeObjectClass = ((RefType) invokeObjectType).getSootClass();
                    if (invokeObjectClass.isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(invokeObjectClass)) {
                        // 如果invokeObjectClass是
                        if (AppAnalyzer.isSubclassOrImplementer(invokeObjectClass, tgtMethod.getDeclaringClass()) && invokeObjectClass.isConcrete()) {
                            tgtMethod = AppAnalyzer.locateMethod(invokeObjectClass, tgtSubSignature);
                        }
                    }
                }

                if (tgtMethod == null) {
                    System.err.println(possibleMethods.size());
                    System.err.println("[Error], cannot find target method.");
                    System.err.println(sootMethod + " => " + stmt);
                    System.exit(0);
                }
            }
            else {
                System.out.println("[Error], multiple edges to static target method.");
                System.out.println(sootMethod + " => " + stmt);
                System.exit(0);
            }
        }
        else {
            // 估计原因是没有找到方法的实现
            tgtMethod = stmt.getInvokeExpr().getMethod();
//            System.err.println("[Error], no target method.");
//            System.err.println(sootMethod + " => " + stmt);
//            System.exit(0);
        }

        record = checkAndAnalyzeNewThread(tgtMethod, in, stmt);
        if (record != null) return record;

        record = checkAndDoIccTransformation(tgtMethod, stmt, context.getTaintedParamIndexes());
        if (record != null) return record;
        record = checkAndAnalyzeLibraryMethod(tgtMethod, context);
        if (record != null) return record;
        record = analyzeNormalMethod(tgtMethod, context);
        return record;
    }

    public MethodAnalysisResult checkAndAnalyzeNewThread(SootMethod tgtMethod, FlowSet<Value> in, Stmt stmt) {
        if (isThreadRunMethod(tgtMethod)) {
            InstanceInvokeExpr threadStartExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
            SootMethod exprMethod = threadStartExpr.getMethod();
            Value runnableObj = null;
            if (exprMethod == mtdThreadStart) {
                runnableObj = threadStartExpr.getBase();
            } else if (exprMethod == mtdRunOnUiThread) {
                runnableObj = threadStartExpr.getArg(0);
            }
            // todo 分析runnableObj的run()方法
            if (in.contains(runnableObj)) {

                ForwardInvokeContext runnableContext = new ForwardInvokeContext(true, tgtMethod.getDeclaringClass().getType(), new ArraySparseSet<>(), new HashMap<>());
                ForwardTaintAnalysis newRunnableAnalysis = new ForwardTaintAnalysis(tgtMethod, runnableContext);
                this.importantInformation.addAll(newRunnableAnalysis.importantInformation);

                return MethodAnalysisResult.emptyResult;
            }
        }

        return null;
    }

    public MethodAnalysisResult checkAndDoIccTransformation(SootMethod tgtMethod, Stmt stmt, FlowSet<Integer> paramIndexes) {
        MethodAnalysisResult record = null;
        if (tgtMethod == AppAnalyzer.startActivityMethod) {
            record = iccTransformer.transform(stmt, paramIndexes);
        }
        else if (tgtMethod == AppAnalyzer.newIntentMethod) {
            // todo 目前仅仅支持new Intent()这种，后续可以支持更多，比如通过封装函数
            iccTransformer.parseIntent(stmt);
            record = MethodAnalysisResult.emptyResult;
        }
        return record;
    }

    public MethodAnalysisResult checkAndAnalyzeLibraryMethod(SootMethod tgtMethod, ForwardInvokeContext context) {
        MethodAnalysisResult originalRecord = null;
        if (!tgtMethod.getDeclaringClass().isApplicationClass()) { // Library classes
            originalRecord = getLibraryClassMethodResult(tgtMethod, context);
        }
        else if (AppAnalyzer.is3rdPartyLibrary(tgtMethod.getDeclaringClass())) { // Third party library classes
            originalRecord = getLibraryClassMethodResult(tgtMethod, context);
        }
        else if (tgtMethod.isNative()) {
            originalRecord = getLibraryClassMethodResult(tgtMethod, context);
        }
        else if (tgtMethod.isPhantom()) {
            originalRecord = getLibraryClassMethodResult(tgtMethod, context);
        }
        return originalRecord;
    }

    public MethodAnalysisResult getLibraryClassMethodResult(SootMethod tgtMethod, ForwardInvokeContext context) {
        // The result is tainted as long as the invoke object is tainted or params are tainted.
        // StringBuilder sb = new StringBuilder(); sb.append("abc");
        // System.out.println(xxx) System.out对象也会被taint？
        boolean isInvokeObjectTainted = context.isThisObjectTainted();
        FlowSet<Integer> taintedParamIndexes = context.getTaintedParamIndexes();

        boolean isResultTainted = false;
        if (!(tgtMethod.getReturnType() == VoidType.v())) {
            if (isInvokeObjectTainted || taintedParamIndexes.size() > 0) {
                isResultTainted = true;
            }
        }

        // todo 对调用对象的taint，先支持一下常用的吧，后面看看有没有什么通用的方法
        if (taintedParamIndexes.size() > 0) {
//            SootClass declaringClass = tgtMethod.getDeclaringClass();
//            // Support Icc
//            if (declaringClass.getName().equals("android.content.Intent") && tgtMethod.getName().startsWith("put")) {
//                isInvokeObjectTainted = true;
//            } else if (declaringClass.getName().equals("android.os.Bundle") && tgtMethod.getName().startsWith("put")) {
//                isInvokeObjectTainted = true;
//            }
            if (!tgtMethod.isStatic()) {
                isInvokeObjectTainted = true;
            }
        }

        FlowSet<Integer> taintedParams = new ArraySparseSet<>();
        if (tgtMethod.getSignature().equals("<java.io.BufferedInputStream: int read(byte[])>")) {
            taintedParams.add(0);
        }

        // 对于特定的函数，比如union, copy，第二个参数值可能会发生变化
        return new MethodAnalysisResult(isInvokeObjectTainted, taintedParams, isResultTainted, null);
    }




    public MethodAnalysisResult analyzeNormalMethod(SootMethod tgtMethod, ForwardInvokeContext forwardInvokeContext) {
        MethodAnalysisResult record = null;

//        if (AppAnalyzer.isAnonymousConstructor(tgtMethod)) {
//            if (AppAnalyzer.isDebugging) {
//                System.out.println("             ------- anonymous constructor " + tgtMethod);
//            }
//            if (!forwardInvokeContext.getTaintedParamIndexes().isEmpty()) {
//                record = new MethodAnalysisResult(true, forwardInvokeContext.getTaintedParamIndexes(), false, null);
//            } else {
//                record = MethodAnalysisResult.emptyResult;;
//            }
//        }
//        else
        if (inAnalysisSet.contains(tgtMethod)) {
            // 处理递归调用的情况
            record = MethodAnalysisResult.emptyResult;
        }
        else if (tgtMethod.isAbstract()) {
            record = MethodAnalysisResult.emptyResult;
        }
//        else if (forwardInvokeContext.getTaintedParamIndexes().isEmpty()) {
//            // 排除没有使用返回值，且仅有调用对象被污染（没有参数被污染的）示例函数调用
//            return MethodAnalysisResult.emptyResult;
//        }

        else {
            ForwardTaintAnalysis newTaintAnalysis = new ForwardTaintAnalysis(tgtMethod, forwardInvokeContext); // todo ----------------
            this.importantInformation.addAll(newTaintAnalysis.importantInformation);
            record = newTaintAnalysis.getAnalysisResult();
        }
//        else {
//            record = MethodAnalysisResult.emptyResult;
//        }

        return record;
    }

    public MethodAnalysisResult getAnalysisResult() {
        boolean isInvokeObjectTainted = false;
        FlowSet<Integer> taintedValues = new ArraySparseSet<>(); // 新被taint的参数
        boolean isResultTainted = false;

        Local thisObjectRef = this.sootMethod.isStatic() ? null : this.sootMethod.retrieveActiveBody().getThisLocal();
        List<Local> parameterLocals = this.sootMethod.retrieveActiveBody().getParameterLocals();
        List<Unit> retUnits = this.graph.getTails();

        FlowSet<Value> aggregatedTaintedValues = new ArraySparseSet<>();

        for (Unit retUnit: retUnits) {
            Stmt retStmt = (Stmt) retUnit;
            FlowSet<Value> finalTaintedValues = getFlowBefore(retStmt);
            aggregatedTaintedValues.union(finalTaintedValues);

            if (!(retStmt instanceof ReturnStmt)) {
                continue;
            }
            ReturnStmt returnStmt = (ReturnStmt) retStmt;
            if (finalTaintedValues.contains(returnStmt.getOp())) {
                isResultTainted = true;
            }
        }

        if (aggregatedTaintedValues.contains(thisObjectRef)) {
            isInvokeObjectTainted = true;
        }
        for (int i = 0; i < parameterLocals.size(); i ++) {
            Local param = parameterLocals.get(i);
            if (!this.initialTaintedValues.contains(param)) {
                if (aggregatedTaintedValues.contains(param)) {
                    taintedValues.add(i);
                }
            }
        }
        // todo get tainted params
        return new MethodAnalysisResult(this.sootMethod, isInvokeObjectTainted, taintedValues, isResultTainted, taintPath);
    }

    public void recordImportantMethod(SootMethod tgtMethod, Stmt stmt) {

        SootClass declaringCls = tgtMethod.getDeclaringClass();
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        if (kvClasses.contains(declaringCls)) {
            if (tgtMethod.getName().startsWith("get") || tgtMethod.getName().startsWith("opt")) {
                if (tgtMethod.getParameterCount() >= 1) {
                    Value arg0 = invokeExpr.getArg(0);
                    if (arg0 instanceof StringConstant) {
//                        System.out.println("    KeyString:" + ((StringConstant) arg0).value);
                        this.importantInformation.add(arg0);
                    }
                }
            }
        }
        else if (declaringCls == clsGson && tgtMethod.getName().equals("fromJson")) {
            if (invokeExpr.getArg(1) instanceof ClassConstant) {
//                System.out.println("    Class:" + invokeExpr.getArg(1).toString());
                this.importantInformation.add(((RefType)((ClassConstant) invokeExpr.getArg(1)).toSootType()).getSootClass());
            }
        }
        // todo 补充其他的KV结构以及转化为Class结构的函数
    }

    public void recordImportantType(AssignStmt assignStmt) {
        Type leftType = assignStmt.getLeftOp().getType();
        Value rightOp = assignStmt.getRightOp();
        if (leftType instanceof RefType) {
            SootClass leftOpClass = ((RefType) leftType).getSootClass();
            if (leftOpClass.isLibraryClass()) return;
            if (leftOpClass.isPhantomClass()) return;
            if (AppAnalyzer.is3rdPartyLibrary(leftOpClass)) return;

            SootClass superClass = leftOpClass.getSuperclassUnsafe();
            if (superClass != Scene.v().getSootClass("java.lang.Object")) {
                if (superClass.isLibraryClass()) return;
                if (AppAnalyzer.is3rdPartyLibrary(superClass)) return;
            }
            if (rightOp instanceof CastExpr) {
                Type castType = ((CastExpr) rightOp).getCastType();
                if (castType instanceof RefType) {
                    importantInformation.add(((RefType) castType).getSootClass());
//                    System.out.println("    Class:" + ((RefType) castType).getClassName());
                }
            }
        }
    }

    public List<SootMethod> parseMethods(Stmt stmt) {
        CallGraph callGraph = Scene.v().getCallGraph();
        Targets targets = new Targets(callGraph.edgesOutOf(stmt));
        List<SootMethod> res = new ArrayList<>();
        String methodName = stmt.getInvokeExpr().getMethod().getName();

        boolean isThreadRunMethod = false;
        while (targets.hasNext()) {
            SootMethod sootMethod = (SootMethod) targets.next();
            if (sootMethod.getName().equals(methodName)) {
                res.add(sootMethod);
            } else if (isThreadRunMethod(sootMethod)) {
                res.add(sootMethod);
                isThreadRunMethod = true;
            }

            if (isThreadRunMethod) {
                // remove raw Thread or Runnable method
                res.remove(mtdThreadStart);
                res.remove(mtdRunOnUiThread);
            }

        }
        return res;
    }

    public boolean isThreadRunMethod(SootMethod sm) {
        if (sm.getSubSignature().equals("void run()")) {
            SootClass sc = sm.getDeclaringClass();
            if (sc.isConcrete()) {
                Hierarchy hierarchy = Scene.v().getActiveHierarchy();
                if (sc.implementsInterface("java.lang.Runnable")) return true;
                if (hierarchy.isClassSubclassOf(sc, clsThread)) return true;
            }
        }
        return false;
    }

    public HashSet<Object> getImportantInformation() {
        return importantInformation;
    }

}
