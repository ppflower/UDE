package ude.binder;

import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.binder.anlysis.BackwardBinderAnalysis;

import java.util.ArrayList;
import java.util.List;

public class HttpClientV5SyncBinder extends Binder implements CallbackLocator {

    public static String[] METHOD_SIGNATURES = {
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: java.lang.Object execute(org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.io.HttpClientResponseHandler)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: java.lang.Object execute(org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.http.io.HttpClientResponseHandler)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: java.lang.Object execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.io.HttpClientResponseHandler)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: java.lang.Object execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext,org.apache.hc.core5.http.io.HttpClientResponseHandler)>",

            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.client5.http.impl.classic.CloseableHttpResponse execute(org.apache.hc.core5.http.ClassicHttpRequest)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.client5.http.impl.classic.CloseableHttpResponse execute(org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.client5.http.impl.classic.CloseableHttpResponse execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.client5.http.impl.classic.CloseableHttpResponse execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext)>",

            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.core5.http.ClassicHttpResponse execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.core5.http.HttpResponse execute(org.apache.hc.core5.http.ClassicHttpRequest)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.core5.http.HttpResponse execute(org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext)>",
            "<org.apache.hc.client5.http.impl.classic.CloseableHttpClient: org.apache.hc.core5.http.HttpResponse execute(org.apache.hc.core5.http.HttpHost,org.apache.hc.core5.http.ClassicHttpRequest,org.apache.hc.core5.http.protocol.HttpContext)>"
    };

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
//        System.out.println(sootMethod);
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethodRef callee = invokeExpr.getMethodRef();
        if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[0])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});
//
//            Type handlerType = invokeExpr.getArg(1).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateStandardCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[1])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateStandardCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[2])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});
//
//            Type handlerType = invokeExpr.getArg(2).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateStandardCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[3])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});
//
//            Type handlerType = invokeExpr.getArg(3).getType();
//            RefType refType = (RefType) handlerType;
//            SootMethod callback = locateStandardCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {0});

            return true;
        }

        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[4])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[5])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[6])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[7])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }

        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[8])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[9])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[10])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        else if (callee.getSignature().equals(HttpClientV5SyncBinder.METHOD_SIGNATURES[11])) {
//            Object[] backwardAnalysisParam = new Object[] {apiInvokeStmt, false, new int[] {0, 1}};
////            super.startBackwardAnalysis(invokeContextMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            super.startForwardAnalysis(invokeContextMethod, apiInvokeStmt, new int[] {});

            return true;
        }
        return false;
    }

    public SootMethod locateStandardCallback(SootClass implementer) {
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
