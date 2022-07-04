package ude.binder;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class HttpUrlConnectionBinder extends SyncBinder {

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<java.net.HttpURLConnection: java.io.InputStream getInputStream()>")) {
            List<Integer> requestIndexes = new ArrayList<>();
            findSyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, requestIndexes, true);
            return true;
        }
        return false;
    }
}
