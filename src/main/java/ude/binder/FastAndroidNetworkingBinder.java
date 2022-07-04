package ude.binder;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.jimple.Stmt;

import java.util.Collections;

public class FastAndroidNetworkingBinder extends AsyncBinder {
    public FastAndroidNetworkingBinder() {
        this.handlerClass = Scene.v().getSootClass("com.androidnetworking.interfaces.JSONObjectRequestListener");
        this.callbackTaintedParamIndexes.add(0);
    }



    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<com.androidnetworking.common.ANRequest: void getAsJSONObject(com.androidnetworking.interfaces.JSONObjectRequestListener)>")) {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, true, null, Collections.singletonList(0));
            return true;
        }

        return false;
    }


    @Override
    public SootMethod locateStandardCallback(SootClass sootClass) {
        SootMethod callback = sootClass.getMethod("void onResponse(org.json.JSONObject)");
        return callback;
    }
}
