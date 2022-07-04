package ude.binder;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.List;

public class HttpClientV4SyncBinder extends Binder {
    public static String[] METHOD_SIGNATURES = {
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.HttpResponse execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.HttpResponse execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest,cz.msebera.android.httpclient.protocol.HttpContext)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.HttpResponse execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.HttpResponse execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest,cz.msebera.android.httpclient.protocol.HttpContext)>",

            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.client.methods.CloseableHttpResponse execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.client.methods.CloseableHttpResponse execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest,cz.msebera.android.httpclient.protocol.HttpContext)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.client.methods.CloseableHttpResponse execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: cz.msebera.android.httpclient.client.methods.CloseableHttpResponse execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest,cz.msebera.android.httpclient.protocol.HttpContext)>",

            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: java.lang.Object execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest,cz.msebera.android.httpclient.client.ResponseHandler)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: java.lang.Object execute(cz.msebera.android.httpclient.HttpHost,cz.msebera.android.httpclient.HttpRequest,cz.msebera.android.httpclient.client.ResponseHandler,cz.msebera.android.httpclient.protocol.HttpContext)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: java.lang.Object execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest,cz.msebera.android.httpclient.client.ResponseHandler)>",
            "<cz.msebera.android.httpclient.impl.client.CloseableHttpClient: java.lang.Object execute(cz.msebera.android.httpclient.client.methods.HttpUriRequest,cz.msebera.android.httpclient.client.ResponseHandler,cz.msebera.android.httpclient.protocol.HttpContext)>"
    };

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
//        System.out.println(sootMethod);
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethodRef callee = invokeExpr.getMethodRef();
        if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[0])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[1])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[2])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[3])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }

        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[4])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[5])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[6])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[7])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }

        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[8])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[9])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[10])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV4SyncBinder.METHOD_SIGNATURES[11])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        return false;
    }

    public SootMethod locateCallback(SootClass implementer) {
        // void completed(T result);
        List<SootMethod> methods = implementer.getMethods();
        SootMethod callback = null;
        int callbackCount = 0;
        for (SootMethod method: methods) {
            if (!method.getName().equals("handleResponse")) continue;
            if (method.getParameterCount() != 1) continue;
            SootClass object = Scene.v().getSootClass("java.lang.Object");
            if (method.getReturnType() == object.getType()) continue;
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
