package ude.binder;

import soot.*;
import soot.jimple.*;

import java.util.Collections;

public class OkHttp3AsyncBinder extends AsyncBinder {

    public OkHttp3AsyncBinder() {
        this.handlerClass = Scene.v().getSootClass("okhttp3.Callback");
        this.callbackTaintedParamIndexes.add(1);
    }

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<okhttp3.Call: void enqueue(okhttp3.Callback)>")) {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, true, null, Collections.singletonList(0));
            return true;
        }
        else if (callee.getSignature().equals("<com.google.firebase.perf.network.FirebasePerfOkHttpClient: void enqueue(okhttp3.Call,okhttp3.Callback)>"))  {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, false, Collections.singletonList(0), Collections.singletonList(1));
            return true;
        }
        return false;
    }


    @Override
    public SootMethod locateStandardCallback(SootClass sootClass) {
        // void onResponse(Call call, Response response)
        SootMethod callback = sootClass.getMethodUnsafe("void onResponse(okhttp3.Call,okhttp3.Response)");
        return callback;
    }

}
