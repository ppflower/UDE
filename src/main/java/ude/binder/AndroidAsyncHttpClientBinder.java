package ude.binder;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AndroidAsyncHttpClientBinder extends AsyncBinder {

    public AndroidAsyncHttpClientBinder() {
        this.handlerClass = Scene.v().getSootClass("com.loopj.android.http.AsyncHttpResponseHandler");
        this.callbackTaintedParamIndexes.add(2);
    }

    public static String[] METHOD_SIGNATURES = {
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(java.lang.String,com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",

            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,org.apache.http.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,org.apache.http.Header[],org.apache.http.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,org.apache.http.Header[],com.loopj.android.http.RequestParams,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",

            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,cz.msebera.android.httpclient.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,cz.msebera.android.httpclient.Header[],cz.msebera.android.httpclient.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle post(android.content.Context,java.lang.String,cz.msebera.android.httpclient.Header[],com.loopj.android.http.RequestParams,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",


            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(java.lang.String,com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",

            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,org.apache.http.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,org.apache.http.Header[],com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",

            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,cz.msebera.android.httpclient.HttpEntity,java.lang.String,com.loopj.android.http.ResponseHandlerInterface)>",
            "<com.loopj.android.http.AsyncHttpClient: com.loopj.android.http.RequestHandle get(android.content.Context,java.lang.String,cz.msebera.android.httpclient.Header[],com.loopj.android.http.RequestParams,com.loopj.android.http.ResponseHandlerInterface)>",
    };

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        /*
        * */
        if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[0])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(0, null), 1);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[1])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(0, 1), 2);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[2])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 2), 3);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[3])) {
            System.out.println(3);
//            System.out.println("正在分析" + callee.getSignature());
//            System.out.println();
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, false, Arrays.asList(1, 2), Collections.singletonList(4));

//            Object[] backwardAnalysisParam = new Object[] {stmt, false, new int[] {1, 2}};
//            super.startBackwardAnalysis(sootMethod, stmt, (int[]) backwardAnalysisParam[2]);
//
//            InvokeExpr invokeExpr = stmt.getInvokeExpr();
//            RefType refType = (RefType) invokeExpr.getArg(4).getType();
//            SootMethod callback = locateCallback(refType.getSootClass());
//            super.startForwardAnalysis(callback, null, new int[] {1});

            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[4])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 5);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[5])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 5);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[6])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 2), 4);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[7])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 5);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[8])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 5);
            return true;
        }

        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[9])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 2), 3);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[10])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, null), 2);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[11])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(0, 1), 2);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[12])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(0, null), 1);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[13])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 2), 4);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[14])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 4);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[15])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 2), 4);
            return true;
        }
        else if (callee.getSignature().equals(AndroidAsyncHttpClientBinder.METHOD_SIGNATURES[16])) {
//            findRequestAndResponseParams(invokeContextMethod, stmt, Arrays.asList(1, 3), 4);
            return true;
        }

        return false;
    }

    @Override
    public SootMethod locateStandardCallback(SootClass sootClass) {
        SootClass currentClass = sootClass;

        do {
            SootMethod sm;
            sm = currentClass.getMethodUnsafe("void onSuccess(int,org.apache.http.Header[],org.json.JSONObject)");
            if (sm != null) return sm;
            sm = currentClass.getMethodUnsafe("void onSuccess(int,cz.msebera.android.httpclient.Header[],org.json.JSONObject)");
            if (sm != null) return sm;

            sm = currentClass.getMethodUnsafe("void onSuccess(int,org.apache.http.Header[],org.json.JSONArray)");
            if (sm != null) return sm;
            sm = currentClass.getMethodUnsafe("void onSuccess(int,cz.msebera.android.httpclient.Header[],org.json.JSONArray)");
            if (sm != null) return sm;

            sm = currentClass.getMethodUnsafe("void onSuccess(int,org.apache.http.Header[],java.lang.String)");
            if (sm != null) return sm;
            sm = currentClass.getMethodUnsafe("void onSuccess(int,cz.msebera.android.httpclient.Header[],java.lang.String)");
            if (sm != null) return sm;

            if (currentClass.hasSuperclass()) {
                currentClass = currentClass.getSuperclass();
            } else {
                break;
            }
        } while (true);

        System.out.println("No callback found in " + sootClass);
        return null;
    }

    public List<Integer> array2list(Integer [] array) {
        return new ArrayList<>(Arrays.asList(array));
    }

}
