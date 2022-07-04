package ude.backward;

//用于提取riskUnit中关键信息

import soot.*;
import soot.jimple.*;
import ude.AppAnalyzer;

import java.util.ArrayList;
import java.util.List;

import static ude.AppAnalyzer.backwardFilterPrefixList;

public class RiskUnitUtils {

    public static List<String> extractImportantInfo(Unit unit) {
        Stmt stmt = (Stmt) unit;
        List<String> res = new ArrayList<>();
        if (stmt instanceof AssignStmt) {
            Value rv = ((AssignStmt) stmt).getRightOp();
            if (rv instanceof FieldRef) {
                res.add("Field:" + ((FieldRef) rv).getField().getSignature());
                Type t = ((FieldRef) rv).getField().getType();
                if (t instanceof RefType) {
                    SootClass sootClass = ((RefType) t).getSootClass();
                    if (sootClass.isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(sootClass)) {
                        AppAnalyzer.classesToRecordFields.add(sootClass);
                    }
                }
            } else if (rv instanceof InvokeExpr && !isUselessInitFunction(((InvokeExpr) rv).getMethod())) {
                SootMethod sootMethod = ((InvokeExpr) rv).getMethod();
                List<Value> args = ((InvokeExpr) rv).getArgs();
                if (sootMethod.getParameterCount() == 2) {
                    Value key = args.get(0);
                    Value val = args.get(1);
                    addKVImportantInfo(key, val, res);
                }
                boolean isUtilInvoke = isUtilInvokeStmt(stmt);
                if (!isUtilInvoke) {
                    //排除 put函数 且有两个参数的函数
                    String sig = ((InvokeExpr) rv).getMethod().getSignature();
                    res.add("Method:" + sig);
                }
            }
            //AssignStmt 提取结束
        } else if (stmt instanceof InvokeStmt && !isUselessInitFunction(stmt.getInvokeExpr().getMethod())) {
            SootMethod sootMethod = stmt.getInvokeExpr().getMethod();
            //判断是否存在非String常量数据，如果存在，则需要将该调用语句打印，否则只需要log StringConstant即可
            //并且考虑是否Key-Value的Map进行标记
            List<Value> args = stmt.getInvokeExpr().getArgs();
            if (sootMethod.getParameterCount() == 2) {
                Value key = args.get(0);
                Value val = args.get(1);
                addKVImportantInfo(key, val, res);
            }
            boolean isUtilInvoke = isUtilInvokeStmt(stmt);
            if (!isUtilInvoke) {
                String sig = sootMethod.getSignature();
                res.add("Method:" + sig);
            }
        }

        return res;
    }

    private static boolean isUselessInitFunction(SootMethod sm) {
        return (sm.getSignature().contains("<init>") && sm.getParameterCount() == 0);
    }

    private static void addKVImportantInfo(Value key, Value val, List<String> importantInfo) {
        if (!key.getType().toString().equals("java.lang.String"))
            return;
        if (key instanceof StringConstant) {
            String tmp = key.toString().replace("\"", "");
            if (!tmp.contains("//")) {
                tmp = tmp.trim();
                tmp = tmp.replaceAll("[^0-9a-zA-Z-_]", "");
                importantInfo.add("KeyString:" + tmp);
            }

        }
    }

    private static boolean isUtilInvokeStmt(Stmt stmt) {
        //如果包名前缀出现在黑名单中，则将其舍去
        if (stmt.containsInvokeExpr()) {
            String pkgName = stmt.getInvokeExpr().getMethod().getDeclaringClass().getPackageName();
            String className = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();

            if (stmt.getInvokeExpr().getMethod().getSignature().contains("<init>"))
                return true;

            return isBackwardFilterClass(className);
        }
        return false;
    }

    public static boolean isBackwardFilterClass(String clzName) {
        for (String backwardFilterPrefix : backwardFilterPrefixList) {
            if (clzName.startsWith(backwardFilterPrefix)) {
                return true;
            }
        }
        return false;
    }
}
