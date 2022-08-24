package ude.binder;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import ude.binder.anlysis.MethodNodeAsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VolleyBinder extends AsyncBinder {

    public VolleyBinder() {
        this.handlerClass = Scene.v().getSootClass("com.android.volley.Response$Listener");
        this.callbackTaintedParamIndexes.add(0);
    }

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
//        Request-><init>(I, String, Response$ErrorListener)
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethodRef callee = apiInvokeStmt.getInvokeExpr().getMethodRef();
        if (callee.getSignature().equals("<com.android.volley.Request: void <init>(int,java.lang.String,com.android.volley.Response$ErrorListener)>")) {
            // 后面看要不要支持
            /*
            * 从CustomRequest init开始分析，分析出request url相关的参数来自上层
            * 然后遍历分析是否传入了Map类型以及Listener类型，如果有着讲当前的调用方法设置为一个WrapperRoot
            * 然后
            * */

            analyzeCustomRequest(invokeContextMethod, apiInvokeStmt);
            return true;
        }
        if (callee.getSignature().equals("<com.android.volley.toolbox.JsonObjectRequest: void <init>(int,java.lang.String,org.json.JSONObject,com.android.volley.Response$Listener,com.android.volley.Response$ErrorListener)>")) {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, false, Arrays.asList(1, 2), Collections.singletonList(3));
            return true;
        }
        else if (callee.getSignature().equals("<com.android.volley.toolbox.StringRequest: void <init>(int,java.lang.String,com.android.volley.Response.Listener<java.lang.String>,com.android.volley.Response.ErrorListener)>")) {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, false, Collections.singletonList(1), Collections.singletonList(2));
            return true;
        } else if (callee.getSignature().equals("<com.android.volley.toolbox.JsonArrayRequest: void <init>(int,java.lang.String,org.json.JSONArray,com.android.volley.Response.Listener<org.json.JSONArray>,com.android.volley.Response.ErrorListener)>")) {
            findAsyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, false, Arrays.asList(1, 2), Collections.singletonList(3));
            return true;
        }
        return false;
    }

    public void analyzeCustomRequest(SootMethod apiInvokeMethod, Stmt apiInvokeStmt) {
        Local urlValue = (Local) apiInvokeStmt.getInvokeExpr().getArg(1);
        Body body = apiInvokeMethod.retrieveActiveBody();
        List<Local> paramLocals = body.getParameterLocals();
        if (apiInvokeMethod.isConstructor()) {
            MethodNodeAsync currentNode = new MethodNodeAsync(apiInvokeMethod, apiInvokeStmt);
//            List<Integer> requestIndexes = new ArrayList<>();
//            List<Integer> responseHandlerIndexes = new ArrayList<>();
            if (paramLocals.contains(urlValue)) {
                // 说明这是一个构造方法（大概率继承了Request类），按理说应该会传入Map以及Listener
                currentNode.getRequestParamsIndexes().add(paramLocals.indexOf(urlValue));
                for (Unit unit: apiInvokeMethod.retrieveActiveBody().getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt instanceof AssignStmt) {
                        Value leftOp = ((AssignStmt) stmt).getLeftOp();
                        Value rightOp = ((AssignStmt) stmt).getRightOp();
                        if ((rightOp instanceof Local) && paramLocals.contains(rightOp)) {
                            if (leftOp instanceof InstanceFieldRef) {
                                Value base = ((InstanceFieldRef) leftOp).getBase();
                                if (base == body.getThisLocal()) {
                                    if (rightOp.getType()==Scene.v().getRefType("java.util.Map")) {
                                         currentNode.getRequestParamsIndexes().add(paramLocals.indexOf(rightOp));
                                    } else if (rightOp.getType()==Scene.v().getRefType("com.android.volley.Response$Listener")) {
                                        currentNode.getResponseHandlerParamsIndexes().add(paramLocals.indexOf(rightOp));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 前面判断当前这个方法是不是一个根节点，希望是一个继承了Request类的类的构造方法
            // todo 开始构建树
            boolean isWrapper = !currentNode.getRequestParamsIndexes().isEmpty() && !currentNode.getResponseHandlerParamsIndexes().isEmpty();
            currentNode.setWrapper(isWrapper);
            if (isWrapper) {
                analyzeEdgesIntoCurrentWrapper(currentNode); // 构建wrapper tree
                analyzeAsyncWrapperTree(currentNode);
            }

        }

    }

    @Override
    public SootMethod locateStandardCallback(SootClass sootClass) {
        // void onResponse(JSONObject response), 做了一定防混淆处理，不依赖于函数名识别
        List<SootMethod> candidates = new ArrayList<>();
        List<SootMethod> methods = sootClass.getMethods();
        SootMethod callback = null;
        int callbackCount = 0;
        for (SootMethod method: methods) {
            if (method.getParameterCount() != 1) continue;
            if (method.getName().equals("<init>")) continue;

            candidates.add(method);
//            callback = method;
//            callbackCount ++;
        }
        if (candidates.size() == 0) {
            System.err.println("!!!!!!!!!!!!!!! Fail to locate callback in " + sootClass + ", no candidate.");
        } else if (candidates.size() == 1) {
            callback = candidates.get(0);
        } else if (candidates.size() == 2 && candidates.get(0).getName().equals(candidates.get(1).getName())) {
            SootClass object = Scene.v().getSootClass("java.lang.Object");
            callback = candidates.get(0);
            if (callback.getParameterType(0) == object.getType()) {
                callback = candidates.get(1);
            }
        } else {
            System.err.println("!!!!!!!!!!!!!!! Fail to locate callback in " + sootClass + ", too many candidates");
            System.err.println(methods);
        }
        return callback;
    }

}
