package ude.backward;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import ude.AppAnalyzer;
import ude.binder.anlysis.MustAliasAnalysis;
import ude.forward.MethodAnalysisResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MyBackwardTaintAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Value>> {

    public static boolean isDebugging;
    private SootMethod sootMethod;
    private BackwardInvokeContext backwardInvokeContext;

    private MustAliasAnalysis mustAliasAnalysis;

    public MyBackwardTaintAnalysis(SootMethod sootMethod, BackwardInvokeContext backwardInvokeContext) {
        super(new BriefUnitGraph(sootMethod.retrieveActiveBody()));

        this.sootMethod = sootMethod;
        this.backwardInvokeContext = backwardInvokeContext;

        if (!AppAnalyzer.analyzedAliases.containsKey(sootMethod)) {
            AppAnalyzer.analyzedAliases.put(sootMethod, new MustAliasAnalysis(sootMethod));
        }
        this.mustAliasAnalysis = AppAnalyzer.analyzedAliases.get(sootMethod);

        doAnalysis();
    }


    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
        Stmt stmt = (Stmt) unit;

        if (isDebugging) {
            System.out.println(String.format("%-15s", "[" + sootMethod.getName() + "] ") + String.format("%-30s", in.toString()) + unit);
        }

        in.copy(out);
        FlowSet<Value> killSet = new ArraySparseSet<>();
        FlowSet<Value> genSet = new ArraySparseSet<>();

        // assign 如果左值在被taint，那么将左值kill掉，将右值使用到的值加到out中（函数调用对象、参数、加减乘除运算变量、类型转换变量、field引用）

        if (stmt instanceof AssignStmt) {
            // Kill the value defined in this unit
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value leftOp = assignStmt.getLeftOp();
            if (leftOp instanceof Local) {
                killSet.add(leftOp);
            }
        }

        MethodAnalysisResult methodAnalysisResult = null;
        if (stmt.containsInvokeExpr()) {
            methodAnalysisResult = analyzeInvoke(in, stmt);
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (invokeExpr instanceof InstanceInvokeExpr) {
                if (methodAnalysisResult.isSelfObjectTainted()) {
                    Value invokeObject = ((InstanceInvokeExpr) invokeExpr).getBase();
                    genSet.add(invokeObject);
                }
            }
            for (Integer i: methodAnalysisResult.getTaintedParams()) {
                Value arg = invokeExpr.getArg(i);
                genSet.add(arg);
            }
        }
        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            if (in.contains(leftOp)) {
                if (rightOp instanceof Local) {
                    genSet.add(rightOp);
                }
                else if (rightOp instanceof InstanceFieldRef) {
                    // todo   val = thisObj.field, val = obj.field
                    Value fieldBase = ((InstanceFieldRef) rightOp).getBase();
                    if (!sootMethod.isStatic() &&fieldBase != sootMethod.retrieveActiveBody().getThisLocal()) {
                        genSet.add(fieldBase);
                    }
                }
                else if (rightOp instanceof ArrayRef) {
                    Value arrayBase = ((ArrayRef) rightOp).getBase();
                    Value index = ((ArrayRef) rightOp).getIndex();
                    if (arrayBase instanceof Local) genSet.add(arrayBase);
                    if (index instanceof Local) genSet.add(index);
                }
                else if (rightOp instanceof CastExpr) {
                    genSet.add(((CastExpr) rightOp).getOp());
                } else if (rightOp instanceof BinopExpr) {
                    Value op1 = ((BinopExpr) rightOp).getOp1();
                    if (op1 instanceof Local) genSet.add(op1);
                    Value op2 = ((BinopExpr) rightOp).getOp2();
                    if (op2 instanceof Local) genSet.add(op2);
                } else if (rightOp instanceof NegExpr) {
                    Value op = ((NegExpr) rightOp).getOp();
                    genSet.add(op);
                }
            }
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

        else if (backwardInvokeContext.isResultTainted()) {
            if (stmt instanceof ReturnStmt) {
                Value returnValue = ((ReturnStmt) stmt).getOp();
                if (returnValue instanceof Local) genSet.add(returnValue);
            }
        }

        for (Value value: out) {
            FlowSet<Value> aliases = getAliasSet(unit, value);
            out.union(aliases);
        }
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected FlowSet<Value> entryInitialFlow() {
//        return new ArraySparseSet<>();
        FlowSet<Value> res = new ArraySparseSet<>();
        if (backwardInvokeContext.isThisObjectTainted()) {
            res.add(sootMethod.retrieveActiveBody().getThisLocal());
        }
        return res;
    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
        source.copy(dest);
    }

    public FlowSet<Value> getRightOpTaintedValues(Value rightOp, MethodAnalysisResult methodAnalysisResult) {
        FlowSet<Value> genSet = new ArraySparseSet<>();
        if (rightOp instanceof Local) {
            genSet.add(rightOp);
        }
        else if (rightOp instanceof InstanceFieldRef) {
            // todo   val = thisObj.field, val = obj.field
            Value fieldBase = ((InstanceFieldRef) rightOp).getBase();
            if (!this.sootMethod.isStatic() &&fieldBase != this.sootMethod.retrieveActiveBody().getThisLocal()) {
                genSet.add(fieldBase);
            }
        }
        else if (rightOp instanceof ArrayRef) {
            Value arrayBase = ((ArrayRef) rightOp).getBase();
            Value index = ((ArrayRef) rightOp).getIndex();
            if (arrayBase instanceof Local) genSet.add(arrayBase);
            if (index instanceof Local) genSet.add(index);
        }
        else if (rightOp instanceof CastExpr) {
            genSet.add(((CastExpr) rightOp).getOp());
        } else if (rightOp instanceof BinopExpr) {
            Value op1 = ((BinopExpr) rightOp).getOp1();
            if (op1 instanceof Local) genSet.add(op1);
            Value op2 = ((BinopExpr) rightOp).getOp2();
            if (op2 instanceof Local) genSet.add(op2);
        } else if (rightOp instanceof NegExpr) {
            Value op = ((NegExpr) rightOp).getOp();
            genSet.add(op);
        }
        return genSet;
    }

    public MethodAnalysisResult analyzeInvoke(FlowSet<Value> in, Stmt stmt) {
        boolean isInvokeResTainted = false;
        if (stmt instanceof AssignStmt) {
            if (in.contains(((AssignStmt) stmt).getLeftOp())) {
                isInvokeResTainted = true;
            }
        }

        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Value> args = stmt.getInvokeExpr().getArgs();
        // ICFG invoke translation
        boolean isInvokeObjectTainted = false;
        RefType invokeObjectType = null;
        if (invokeExpr instanceof InstanceInvokeExpr) {
            Local invokeObject = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
            if (in.contains(invokeObject)) {
                isInvokeObjectTainted = true;
                invokeObjectType = (RefType) invokeObject.getType();
            }
            // 获取更加具体的this类型
            Body body = sootMethod.retrieveActiveBody();
            if (!sootMethod.isStatic() && invokeObject == body.getThisLocal()) {
                invokeObjectType = this.backwardInvokeContext.getThisObjectType();
            }
            else if (body.getParameterLocals().contains(invokeObject)) {
                List<Local> paramLocals = sootMethod.retrieveActiveBody().getParameterLocals();
                int index = paramLocals.indexOf(invokeObject);
                Value arg = this.backwardInvokeContext.getParamIndex2Arg().get(index);
                if (arg != null) {
                    invokeObjectType = (RefType) arg.getType();
                } else {
                    invokeObjectType = (RefType) invokeObject.getType();
                }
            }
            else {
                invokeObjectType = (RefType) invokeObject.getType();
            }
        }

//        FlowSet<Integer> taintedParamIndexes = new ArraySparseSet<>();
        HashMap<Integer, Value> paramIndex2Arg = new HashMap<>();
        for (int i = 0; i < args.size(); i ++) {
            Value arg = args.get(i);
//            if (in.contains(arg)) {
//                taintedParamIndexes.add(i);
//            }
            paramIndex2Arg.put(i, arg);
        }

        if (!isInvokeResTainted && !isInvokeObjectTainted) {
            return MethodAnalysisResult.emptyResult;
        }

        BackwardInvokeContext context = new BackwardInvokeContext(isInvokeObjectTainted, invokeObjectType, isInvokeResTainted, paramIndex2Arg);

        return analyzeMethod(stmt, context);

    }

    public MethodAnalysisResult analyzeMethod(Stmt stmt, BackwardInvokeContext context) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();

        List<SootMethod> possibleMethods = parseMethods(stmt);
        SootMethod tgtMethod = invokeExpr.getMethod();;
        MethodAnalysisResult record;
        if (possibleMethods.size() == 1) {
            tgtMethod = possibleMethods.get(0);
        }
        else if (possibleMethods.size() >= 2) {
            // todo 如果存在多条边，需要确认具体是哪个方法
            if (invokeExpr instanceof InstanceInvokeExpr) {
                String tgtSubSignature = invokeExpr.getMethod().getSubSignature();
                Local invokeObject = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
                RefType invokeObjectType;

                List<Local> paramLocals = sootMethod.retrieveActiveBody().getParameterLocals();
                if (paramLocals.contains(invokeObject)) {
                    int i = paramLocals.indexOf(invokeObject);
                    Value arg = context.getParamIndex2Arg().get(i);
                    invokeObjectType = (RefType) arg.getType();
                }
                else if (invokeObject == sootMethod.retrieveActiveBody().getThisLocal()) {
                    invokeObjectType = context.getThisObjectType();
                }
                else {
                    invokeObjectType = (RefType) invokeObject.getType();
                }
                SootClass invokeObjectClass = invokeObjectType.getSootClass();

                if (invokeObjectClass.isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(invokeObjectClass)) {
                    tgtMethod = AppAnalyzer.locateMethod(invokeObjectClass, tgtSubSignature);
                } else {
                    tgtMethod = invokeExpr.getMethod();
                }

                if (tgtMethod == null) {
                    System.out.println(possibleMethods.size());
                    System.out.println("[Error], cannot find target method.");
                    System.out.println(sootMethod + " => " + stmt);
                    System.exit(0);
                }
            } else {
                System.out.println("[Error], multiple edges to static target method.");
                System.out.println(sootMethod + " => " + stmt);
                System.exit(0);
            }
        } else {
            System.err.println("[Error], no target method.");
            System.out.println(sootMethod + " => " + stmt);
            System.exit(0);
        }

        record = checkAndAnalyzeLibraryMethod(tgtMethod, context);
        if (record != null) return record;

        record = analyzeNormalMethod(tgtMethod, context);
        return record;
    }

    public MethodAnalysisResult checkAndAnalyzeLibraryMethod(SootMethod tgtMethod, BackwardInvokeContext backwardInvokeContext) {
        MethodAnalysisResult originalRecord = null;
        if (!tgtMethod.getDeclaringClass().isApplicationClass()) { // Library classes
            originalRecord = getLibraryClassMethodResult(tgtMethod, backwardInvokeContext);
        }
        else if (AppAnalyzer.is3rdPartyLibrary(tgtMethod.getDeclaringClass())) { // Third party library classes
            originalRecord = getLibraryClassMethodResult(tgtMethod, backwardInvokeContext);
        }
        else if (tgtMethod.isNative()) {
            originalRecord = getLibraryClassMethodResult(tgtMethod, backwardInvokeContext);
        }
        else if (tgtMethod.isPhantom()) {
            originalRecord = getLibraryClassMethodResult(tgtMethod, backwardInvokeContext);
        }
        return originalRecord;
    }

    public MethodAnalysisResult getLibraryClassMethodResult(SootMethod tgtMethod, BackwardInvokeContext backwardInvokeContext) {
        boolean isInvokeObjectTainted = backwardInvokeContext.isThisObjectTainted();
        boolean isResultTainted = backwardInvokeContext.isResultTainted();

        FlowSet<Integer> taintedParamIndexes = new ArraySparseSet<>();
        if (isResultTainted) {
            if (!tgtMethod.isStatic()) {
                isInvokeObjectTainted = true;
            }
            for (int i = 0; i < tgtMethod.getParameterCount(); i ++) {
                taintedParamIndexes.add(i);
            }
        }
        else if (isInvokeObjectTainted) {
            for (int i = 0; i < tgtMethod.getParameterCount(); i ++) {
                taintedParamIndexes.add(i);
            }
        }

        // 对于特定的函数，比如union, copy，第二个参数值可能会发生变化
        return new MethodAnalysisResult(isInvokeObjectTainted, taintedParamIndexes, isResultTainted, null);
    }

    public MethodAnalysisResult analyzeNormalMethod(SootMethod tgtMethod, BackwardInvokeContext backwardInvokeContext) {
        MethodAnalysisResult record = null;

        if (tgtMethod.isAbstract()) {
            record = MethodAnalysisResult.emptyResult;
        }
        else if (tgtMethod.isConcrete()) {
            MyBackwardTaintAnalysis newTaintAnalysis = new MyBackwardTaintAnalysis(tgtMethod, backwardInvokeContext); // todo ----------------
//            this.importantInformation.addAll(newTaintAnalysis.importantInformation);
            record = newTaintAnalysis.getAnalysisResult();
        }
        else {
            record = MethodAnalysisResult.emptyResult;
        }

        return record;
    }

    public MethodAnalysisResult getAnalysisResult() {
        return null;
    }

    public List<SootMethod> parseMethods(Stmt stmt) {
        CallGraph callGraph = Scene.v().getCallGraph();
        Targets targets = new Targets(callGraph.edgesOutOf(stmt));
        List<SootMethod> res = new ArrayList<>();
        String methodName = stmt.getInvokeExpr().getMethod().getName();
        while (targets.hasNext()) {
            SootMethod sootMethod = (SootMethod) targets.next();
            if (sootMethod.getName().equals(methodName)) res.add(sootMethod);
        }
        return res;
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

}
