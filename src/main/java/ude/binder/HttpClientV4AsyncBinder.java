package ude.binder;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.List;

public class HttpClientV4AsyncBinder extends AsyncBinder {

    public HttpClientV4AsyncBinder() {
//        this.handlerClass = Scene.v().getSootClass("com.androidnetworking.interfaces.JSONObjectRequestListener");
//        this.callbackTaintedIndexes.add(0);
    }

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<org.apache.http.impl.nio.client.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.http.client.methods.HttpUriRequest,org.apache.http.concurrent.FutureCallback)>")) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else {
////                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        else if (callee.getSignature().equals("<org.apache.http.impl.nio.client.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.http.client.methods.HttpUriRequest,org.apache.http.concurrent.FutureCallback)>")) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else {
//                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        return false;
    }

    @Override
    public SootMethod locateStandardCallback(SootClass implementer) {
        // void completed(T result);
        List<SootMethod> methods = implementer.getMethods();
        SootMethod callback = null;
        int callbackCount = 0;
        for (SootMethod method: methods) {
            if (!method.getName().equals("completed")) continue;
            if (method.getParameterCount() != 1) continue;
            SootClass object = Scene.v().getSootClass("java.lang.Object");
            if (method.getParameterType(0) == object.getType()) continue;
            callback = method;
            callbackCount ++;
        }
        if (callbackCount != 1) {
            System.err.println("!!!!!!!!!!!!!!! Fail to locate callback in " + implementer);
            System.err.println(methods);
        }
        return callback;
    }

}
