package ude.binder;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.List;

public class HttpClientV5AsyncBinder extends AsyncBinder {

    public HttpClientV5AsyncBinder() {
//        this.handlerClass = Scene.v().getSootClass("com.androidnetworking.interfaces.JSONObjectRequestListener");
//        this.callbackTaintedIndexes.add(0);
    }

    public static String[] METHOD_SIGNATURES = {
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.client5.http.async.methods.SimpleHttpRequest,org.apache.hc.core5.concurrent.FutureCallback)>",
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.client5.http.async.methods.SimpleHttpRequest,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.concurrent.FutureCallback)>",
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.nio.AsyncRequestProducer,org.apache.hc.core5.http.nio.AsyncResponseConsumer,org.apache.hc.core5.http.nio.HandlerFactory,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.concurrent.FutureCallback)>",
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.core5.http.nio.AsyncRequestProducer,org.apache.hc.core5.http.nio.AsyncResponseConsumer,org.apache.hc.core5.concurrent.FutureCallback)>",
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.core5.http.nio.AsyncRequestProducer,org.apache.hc.core5.http.nio.AsyncResponseConsumer,org.apache.hc.core5.http.nio.HandlerFactory,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.concurrent.FutureCallback)>",
            "<org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient: java.util.concurrent.Future execute(org.apache.hc.core5.http.nio.AsyncRequestProducer,org.apache.hc.core5.http.nio.AsyncResponseConsumer,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.concurrent.FutureCallback)>"
    };


    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethodRef callee = invokeExpr.getMethodRef();
        if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[0])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
//            } else {
//                super.startForwardAnalysis(null, null, null);
////                System.out.println("No Forward Analysis");
//            }

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[1])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
//            } else {
//                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[2])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(5).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
//            } else {
//                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[3])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
//            } else {
//                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[4])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(4).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
//            } else {
//                super.startForwardAnalysis(null, null, null);
//            }

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5AsyncBinder.METHOD_SIGNATURES[5])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(3).getType();
//            if (handlerType != NullType.v()) {
//                RefType refType = (RefType) handlerType;
//                SootMethod callback = locateStandardCallback(refType.getSootClass());
//                super.startForwardAnalysis(callback, null, new int[] {0});
//            } else if (apiInvokeStmt instanceof AssignStmt) {
//                // 异步调用的同步用法
//                super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[]{});
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
