package ude.forward;

import soot.*;
import soot.jimple.ClassConstant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.AppAnalyzer;

import java.util.HashMap;

public class IccTransformer {

    public HashMap<Value, SootClass> intent2TargetActivity;

    public IccTransformer() {
        intent2TargetActivity = new HashMap<>();
    }

    public void addIntent(Value intent, Value classValue) {
        if (classValue instanceof ClassConstant) {
            ClassConstant classConstant = (ClassConstant) classValue;
            SootClass targetActivityClass = ((RefType) classConstant.toSootType()).getSootClass();
            intent2TargetActivity.put(intent, targetActivityClass);
        }
    }

    public MethodAnalysisResult transform(Unit unit, FlowSet<Integer> paramIndexes) {
        MethodAnalysisResult record = null;
        // 当前只支持startActivity方法
        // todo 支持startActivityForResult等一些其他方法
        if (paramIndexes.isEmpty()) {
            record = MethodAnalysisResult.emptyResult;
        } else {
            Stmt stmt = (Stmt) unit;
            InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
            Value intentValue = invokeExpr.getArg(0);
            SootClass targetActivityClass = intent2TargetActivity.get(intentValue);
            if (targetActivityClass != null) {
                SootMethod transformedMethod = AppAnalyzer.locateMethod(targetActivityClass, "void onCreate(android.os.Bundle)");
                if (transformedMethod != null) {
                    FlowSet<Integer> taintedParamIndexes = new ArraySparseSet<>();
                    taintedParamIndexes.add(0);
//                    FlowSet<Type> taintedParamTypes = new ArraySparseSet<>();
//                    taintedParamTypes.add(Scene.v().getType("android.os.Bundle"));
                    HashMap<Integer, Value> paramIndex2Arg = new HashMap<>();
                    paramIndex2Arg.put(0, null);
                    ForwardInvokeContext forwardInvokeContext = new ForwardInvokeContext(true, targetActivityClass.getType(), taintedParamIndexes, paramIndex2Arg);
                    ForwardTaintAnalysis forwardTaintAnalysis = new ForwardTaintAnalysis(transformedMethod, forwardInvokeContext); // todo
                    record = forwardTaintAnalysis.getAnalysisResult(); // todo 对onCreate方法分析，给record赋值
                }
                else {
                    record = MethodAnalysisResult.emptyResult;
                }
            } else {
                record = MethodAnalysisResult.emptyResult;
            }
        }
        return record;
    }

    public void parseIntent(Unit unit) {
        Stmt stmt = (Stmt) unit;
        InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
        Value intent = invokeExpr.getBase();
        Value classValue = invokeExpr.getArg(1);
        addIntent(intent, classValue);
    }


}
