package ude.binder;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.binder.anlysis.MethodNodeSync;
import ude.binder.anlysis.ParamPassAnalysis;
import ude.binder.anlysis.ResultPassAnalysis;

import java.util.*;

public class SyncBinder extends Binder {
    static boolean isDebugging = false;

    protected HashSet<SootMethod> wrapperMethods = new HashSet<>();
    protected void findSyncWrappersAndAnalyze(SootMethod apiInvokeMethod, Stmt apiInvokeStmt, List<Integer> requestIndexes, boolean isInvokeObjectRequestRelated) {
        if (!(apiInvokeStmt instanceof AssignStmt))
            return;

        if (isDebugging)
            System.out.println("分析方法 " + apiInvokeMethod + " ==> " + apiInvokeStmt.getInvokeExpr().getMethod());

        MethodNodeSync wrapperNodesRoot = analyzeSyncNetworkApiWrappers(apiInvokeMethod, apiInvokeStmt, isInvokeObjectRequestRelated, requestIndexes);
        if (!wrapperNodesRoot.isWrapper()) {
            // todo 直接分析，不用找wrapper
            if (isDebugging)
                System.out.println("No Wrapper, caller " + apiInvokeMethod);

            analyzeStandardApi(apiInvokeMethod, apiInvokeStmt, isInvokeObjectRequestRelated, requestIndexes, wrapperNodesRoot);
        }
        else {
            analyzeSyncWrapperTree(wrapperNodesRoot);
        }

        wrapperMethods.clear();
    }

    protected void analyzeStandardApi(SootMethod apiInvokeMethod, Stmt apiInvokeStmt, boolean isInvokeObjectRequestRelated, List<Integer> requestIndexes, MethodNodeSync wrapperNodeRoot) {
        List<Value> args = apiInvokeStmt.getInvokeExpr().getArgs();
        FlowSet<Value> apiRequestValues = new ArraySparseSet<>();
        if (requestIndexes != null) {
            for (Integer i: requestIndexes) {
                apiRequestValues.add(args.get(i));
            }
        }
        if (isInvokeObjectRequestRelated) {
            apiRequestValues.add(((InstanceInvokeExpr) apiInvokeStmt.getInvokeExpr()).getBase());
        }
        super.startBackwardAnalysis(apiInvokeMethod, apiInvokeStmt, apiRequestValues, null);
        super.startForwardAnalysisFromStmt(wrapperNodeRoot);
    }

    protected void analyzeSyncWrapperTree(MethodNodeSync rootNode) {
        HashSet<MethodNodeSync> wrapperNodes = new HashSet<>();
        Queue<MethodNodeSync> queue = new LinkedList<>();
        queue.add(rootNode);
        // 确定真正的wrapper方法，对于从leaf到root的任意路径上的节点函数，如果一个函数被调用次数是最多的，那么将它判断为一个wrapper
        while (!queue.isEmpty()) {
            MethodNodeSync wrapperNode = queue.remove();
            List<MethodNodeSync> predecessors = wrapperNode.getPredecessors();
            for (MethodNodeSync predecessor: predecessors) {
                if (predecessor.isWrapper()) {
                    queue.add(predecessor);
                }
            }

            if (!wrapperNode.getInvokeEdges().isEmpty()) {
                // 叶子节点
                MethodNodeSync currentWrapper = wrapperNode;
                MethodNodeSync tmpWrapper = wrapperNode;
                int tmpWrapperCallerCount = tmpWrapper.getPredecessors().size();

                do {
                    int currentCallerCount = currentWrapper.getPredecessors().size();
                    if (currentCallerCount > tmpWrapperCallerCount) {
                        tmpWrapper = currentWrapper;
                        tmpWrapperCallerCount = currentCallerCount;
                    }
                    currentWrapper = currentWrapper.getSuccessor();
                } while (currentWrapper != null);
                tmpWrapper.setFlag(true);
                wrapperNodes.add(tmpWrapper);
            }
        }
        // 同一条路径上可能有两个wrapper（可能有这种情况），如果发现这种情况，把下面那个wrapper在当前路径上的调用边删掉
        for (MethodNodeSync wrapperNode: wrapperNodes) {
            MethodNodeSync current = wrapperNode;
            while (current != null) {
                MethodNodeSync next = current.getSuccessor();
                if (next != null && next.getFlag()) { // 同一条路径上发生冲突了，把successor在这条path上的被调用边删除
                    next.getPredecessors().remove(current);
                }
                current = next;
            }
        }
        for (MethodNodeSync wrapperNode: wrapperNodes) {
            if (isDebugging) {
                System.out.println("Wrapper: " + wrapperNode.getMethod());
                System.out.println("RequestParamIndexes " + wrapperNode.getRequestParamsIndexes());
            }
            for (MethodNodeSync predecessor: wrapperNode.getPredecessors()) {
                if (isDebugging) {
                    System.out.println("Caller: " + predecessor.getMethod());
                }
                SootMethod wrapperInvokeMethod = predecessor.getMethod();
                Stmt wrapperInvokeStmt = predecessor.getInvokeSuccessorStmt();

                List<Value> args = wrapperInvokeStmt.getInvokeExpr().getArgs();
                FlowSet<Value> wrapperRequestValues = new ArraySparseSet<>();
                for (Integer i: wrapperNode.getRequestParamsIndexes()) {
                    wrapperRequestValues.add(args.get(i));
                }
                if (wrapperNode.isInvokeObjectRequestRelated()) {
                    wrapperRequestValues.add(((InstanceInvokeExpr) wrapperInvokeStmt.getInvokeExpr()).getBase());
                }

                super.startBackwardAnalysis(wrapperInvokeMethod, wrapperInvokeStmt, wrapperRequestValues, null);
                super.startForwardAnalysisFromStmt(predecessor);
            }

            if (isDebugging)
                System.out.println("\n");
        }
    }

    protected MethodNodeSync analyzeSyncNetworkApiWrappers(SootMethod currentMethod,
                                                           Stmt stmt,
                                                           boolean isInvokeObjectRequestRelated,
                                                           List<Integer> requestParamIndexes) {
        wrapperMethods.add(currentMethod);
        // todo 建立调用树
        MethodNodeSync currentNode = new MethodNodeSync(currentMethod, stmt);
        InvokeExpr invokeExpr = stmt.getInvokeExpr();

        // 判断stmt中函数调用的实际参数是否与currentMethod的形式参数有关
        FlowSet<Value> requestValues = new ArraySparseSet<>();
        if (requestParamIndexes != null) {
            for (Integer requestParamIndex: requestParamIndexes) {
                Value requestParam = invokeExpr.getArg(requestParamIndex);
                requestValues.add(requestParam);
            }
        }
        if (isInvokeObjectRequestRelated) {
            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
            requestValues.add(instanceInvokeExpr.getBase());
        }
        ParamPassAnalysis requestParamAnalysis = new ParamPassAnalysis(currentMethod, stmt, requestValues, false, false);
        List<ParameterRef> requestParameterRefs = requestParamAnalysis.getTaintedParams();
        boolean passFromWrapperParams = false;
        for (ParameterRef parameterRef: requestParameterRefs) {
            currentNode.getRequestParamsIndexes().add(parameterRef.getIndex());
            passFromWrapperParams = true;
        }
        boolean invokeObjectRequestRelated = requestParamAnalysis.isInvokeObjectTainted();
        currentNode.setInvokeObjectRequestRelated(invokeObjectRequestRelated);

        // ================================================================
        boolean passToWrapperResult = false;
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            FlowSet<Value> startStmtTaintedValues = new ArraySparseSet<>();
            startStmtTaintedValues.add(leftOp);
            ResultPassAnalysis analysis = new ResultPassAnalysis(currentMethod, stmt, startStmtTaintedValues);
            if (analysis.isReturnValueTainted()) {
                passToWrapperResult = true;
            }
            HashSet<Object> currentForwardInfo = analysis.getImportantInformation();
            currentNode.getLocalResponseImportantInfo().addAll(currentForwardInfo);
        }
//        if (currentMethod.getReturnType() instanceof RefType) {}???????????????????
        // 判断currentMethod的结果是否与stmt中函数调用的结果有关
//        boolean passToWrapperResult = ResultPassAnalysis.passToWrapperResult(currentMethod, stmt);

        // ================================================================

        boolean isWrapper = (passFromWrapperParams || invokeObjectRequestRelated) && passToWrapperResult;
        currentNode.setWrapper(isWrapper);
        if (isWrapper) {
            analyzeEdgesIntoCurrentWrapper(currentNode);
        }
        return currentNode;
    }

    protected void analyzeEdgesIntoCurrentWrapper(MethodNodeSync currentNode) {
        SootMethod currentMethod = currentNode.getMethod();
        CallGraph callGraph = Scene.v().getCallGraph();
        Iterator<Edge> edges = callGraph.edgesInto(currentMethod);

        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod callerMethod = edge.src();
            if (callerMethod == currentMethod) continue; // 排除递归
            if (wrapperMethods.contains(callerMethod)) continue; // 排除递归
            Stmt callerStmt = edge.srcStmt();
            SootMethod calleeMethod = callerStmt.getInvokeExpr().getMethod();
            if (calleeMethod != currentMethod) continue; // 排除异步任务调用带来的多余的调用边

            if (isDebugging) {
                System.out.println("分析方法 " + callerMethod + " ==> " + currentMethod);
            }

            MethodNodeSync callerNode = analyzeSyncNetworkApiWrappers(callerMethod, callerStmt, currentNode.isInvokeObjectRequestRelated(), currentNode.getRequestParamsIndexes());
            if (!callerNode.isWrapper()) {
                currentNode.addInvokeEdge(edge);
            }
            currentNode.addPredecessor(callerNode);
            callerNode.setSuccessor(currentNode);

        }
    }

}
