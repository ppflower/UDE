package ude.binder;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class OkHttp3SyncBinder extends SyncBinder {
    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<okhttp3.Call: okhttp3.Response execute()>")) {
            List<Integer> requestIndexes = new ArrayList<>();
            findSyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, requestIndexes, true);
            return true;
        }
        else if (callee.getSignature().equals("<com.google.firebase.perf.network.FirebasePerfOkHttpClient: okhttp3.Response execute(okhttp3.Call)>"))  {
            List<Integer> requestIndexes = new ArrayList<>();
            requestIndexes.add(0);
            findSyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, requestIndexes, false);
            return true;
        }
        return false;
    }
}
