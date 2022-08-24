package ude.binder;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import ude.binder.anlysis.MethodNodeAsync;
import ude.binder.anlysis.ParamPassAnalysis;
import ude.forward.ForwardInvokeContext;
import ude.forward.ForwardTaintAnalysis;

import java.util.*;

public abstract class AsyncBinder extends Binder {
    static boolean isDebugging = false;
    protected SootClass handlerClass;
    protected FlowSet<Integer> callbackTaintedParamIndexes;

    public AsyncBinder() {
        this.callbackTaintedParamIndexes = new ArraySparseSet<>();
    }

    protected void findAsyncWrappersAndAnalyze(SootMethod apiInvokeMethod, Stmt apiInvokeStmt, boolean isInvokeObjectRequestRelated, List<Integer> requestIndexes, List<Integer> responseHandlerIndexes) {

        if (isDebugging)
            System.out.println("分析方法 " + apiInvokeMethod + " ==> " + apiInvokeStmt.getInvokeExpr().getMethod());

        MethodNodeAsync wrapperNodesRoot = analyzeAsyncNetworkApiWrappers(apiInvokeMethod, apiInvokeStmt, isInvokeObjectRequestRelated, requestIndexes, responseHandlerIndexes);

        if (!wrapperNodesRoot.isWrapper()) {
            // todo 直接分析，不用找wrapper
            if (isDebugging)
                System.out.println("No Wrapper, caller " + apiInvokeMethod);
            analyzeStandardApi(apiInvokeMethod, apiInvokeStmt, isInvokeObjectRequestRelated, requestIndexes, wrapperNodesRoot);
        }
        else {
            analyzeAsyncWrapperTree(wrapperNodesRoot);
        }
    }


    protected void analyzeStandardApi(SootMethod apiInvokeMethod, Stmt apiInvokeStmt, boolean isInvokeObjectRequestRelated, List<Integer> requestIndexes, MethodNodeAsync wrapperNodesRoot) {
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
        super.startForwardAnalysisFromCallback(wrapperNodesRoot);
    }


    protected void analyzeAsyncWrapperTree(MethodNodeAsync rootNode) {
        // 从构建好的节点树的根出发，确定合适的包装函数，进行前后向分析
        HashSet<MethodNodeAsync> wrapperNodes = new HashSet<>();
        Queue<MethodNodeAsync> queue = new LinkedList<>();
        queue.add(rootNode);
        // 确定真正的wrapper方法，对于从leaf到root的任意路径上的节点函数，如果一个函数被调用次数是最多的，那么将它判断为一个wrapper
        while (!queue.isEmpty()) {
            MethodNodeAsync wrapperNode = queue.remove();
            List<MethodNodeAsync> predecessors = wrapperNode.getPredecessors();
            for (MethodNodeAsync predecessor: predecessors) {
                if (predecessor.isWrapper()) {
                    queue.add(predecessor);
                }
            }

            if (!wrapperNode.getInvokeEdges().isEmpty()) {
                // 叶子节点
                MethodNodeAsync currentWrapper = wrapperNode;
                MethodNodeAsync tmpWrapper = wrapperNode;
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
        for (MethodNodeAsync wrapperNode: wrapperNodes) {
            MethodNodeAsync current = wrapperNode;
            while (current != null) {
                MethodNodeAsync next = current.getSuccessor();
                if (next != null && next.getFlag()) { // 同一条路径上发生冲突了，把successor在这条path上的被调用边删除
                    next.getPredecessors().remove(current);
                }
                current = next;
            }
        }
        for (MethodNodeAsync wrapperNode: wrapperNodes) {
            if (isDebugging) {
                System.out.println("Wrapper: " + wrapperNode.getMethod());
                System.out.println("RequestParamIndexes " + wrapperNode.getRequestParamsIndexes());
                System.out.println("ResponseHandlerParamIndexes " + wrapperNode.getResponseHandlerParamsIndexes());
            }
            for (MethodNodeAsync predecessor: wrapperNode.getPredecessors()) {
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
                super.startForwardAnalysisFromCallback(predecessor);
            }
            if (isDebugging) {
                System.out.println("\n");
            }
        }
    }


    protected MethodNodeAsync analyzeAsyncNetworkApiWrappers(SootMethod currentMethod, Stmt stmt, boolean isInvokeObjectRequestRelated,
                                                             List<Integer> requestParamIndexes, List<Integer> responseHandlerParamIndexes) {
        // todo 建立调用树
        MethodNodeAsync currentNode = new MethodNodeAsync(currentMethod, stmt);
        InvokeExpr invokeExpr = stmt.getInvokeExpr();

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
        boolean requestValuesPassFromWrapperParams = false;
        for (ParameterRef parameterRef: requestParameterRefs) {
            currentNode.getRequestParamsIndexes().add(parameterRef.getIndex());
            if (parameterRef.getType() instanceof RefType)
                requestValuesPassFromWrapperParams = true;
        }
        boolean invokeObjectRequestRelated = requestParamAnalysis.isInvokeObjectTainted();
        currentNode.setInvokeObjectRequestRelated(invokeObjectRequestRelated);

        // ================================================================
        FlowSet<Value> responseHandlerValues = new ArraySparseSet<>();
        if (responseHandlerParamIndexes != null) {
            for (Integer responseHandlerParamIndex: responseHandlerParamIndexes) {
                Value responseHandlerArg = invokeExpr.getArg(responseHandlerParamIndex);
                responseHandlerValues.add(responseHandlerArg);
            }
        }

        // 分析当前节点的回调函数里提供的信息
        Value handlerValue = responseHandlerValues.toList().get(0); // 当前节点传给下一级的handler的具体值
        if (handlerValue.getType() != NullType.v()) {
            SootClass handlerClass = ((RefType) handlerValue.getType()).getSootClass();
            if (handlerClass.isConcrete()) {
                HashSet<Object> handlerInfo = analyzePossibleCallbacks(handlerClass);
                currentNode.getLocalResponseImportantInfo().addAll(handlerInfo);
            }
        }

        ParamPassAnalysis responseParamAnalysis = new ParamPassAnalysis(currentMethod, stmt, responseHandlerValues, false, false);
        List<ParameterRef> responseParameterRefs = responseParamAnalysis.getTaintedParams();
        boolean responseHandlerValuesPassFromWrapperParams = false;
        for (ParameterRef parameterRef: responseParameterRefs) {
            // 这里应该进一步限制一下，必须是回调类型才行
            if (parameterRef.getType() instanceof RefType) {
                RefType responseHandlerRelatedParam = (RefType) parameterRef.getType();
                SootClass responseHandlerRelatedParamClass = responseHandlerRelatedParam.getSootClass();
                if (isPossibleAbstractNetworkCallbackClass(responseHandlerRelatedParamClass)) {
                    // todo 这里的判断条件还要进一步改
//                    System.out.println("      handler class is " + responseHandlerRelatedParamClass.getName());
                    currentNode.getResponseHandlerParamsIndexes().add(parameterRef.getIndex());
                    responseHandlerValuesPassFromWrapperParams = true;
                }
            }
        }
        if (currentNode.getResponseHandlerParamsIndexes().size() > 1) {
            currentNode.setWrapper(false);
            System.err.println("More handler!!!!! in " + currentMethod);
            return currentNode;
        }
        // ================================================================

        boolean isWrapper = (requestValuesPassFromWrapperParams || isInvokeObjectRequestRelated) && responseHandlerValuesPassFromWrapperParams;
        currentNode.setWrapper(isWrapper);
        if (isWrapper) {
            analyzeEdgesIntoCurrentWrapper(currentNode);
        }
        return currentNode;
    }


    // 和上面一个方法一起构建wrapper tree
    protected void analyzeEdgesIntoCurrentWrapper(MethodNodeAsync currentNode) {
        SootMethod currentMethod = currentNode.getMethod();
        CallGraph callGraph = Scene.v().getCallGraph();
        Iterator<Edge> edges = callGraph.edgesInto(currentMethod);

        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod callerMethod = edge.src();
            if (callerMethod == currentMethod) continue;
            Stmt callerStmt = edge.srcStmt();
            SootMethod calleeMethod = callerStmt.getInvokeExpr().getMethod();
            if (calleeMethod != currentMethod) continue; // 排除异步任务调用带来的多余的调用边

            if (isDebugging)
                System.out.println("分析方法 " + callerMethod + " ==> " + currentMethod);

            Value handlerArg = callerStmt.getInvokeExpr().getArg(currentNode.getResponseHandlerParamsIndexes().get(0));
            Type handlerType = handlerArg.getType();
            if (handlerType == NullType.v()) continue; // handler为null，没有继续分析这个调用的必要了
            if (!(handlerType instanceof RefType)) { // 可能有一些没有碰到的意外情况，
                System.err.println("Unknown handler type found in " + callerMethod + " ==> " + currentMethod);
                System.exit(0);
            }

            MethodNodeAsync callerNode = analyzeAsyncNetworkApiWrappers(callerMethod, callerStmt, false,
                    currentNode.getRequestParamsIndexes(), currentNode.getResponseHandlerParamsIndexes()); // 这里默认只有第一层API会存在调用对象本身是和Request相关的，后面的都是从参数里传进来的
            if (!callerNode.isWrapper()) {
                currentNode.addInvokeEdge(edge);
            }
            currentNode.addPredecessor(callerNode);
            callerNode.setSuccessor(currentNode);
        }
    }


    protected abstract SootMethod locateStandardCallback(SootClass sootClass);


    public List<SootMethod> locatePossibleCallbacks(SootClass sc) {
        List<SootMethod> res = new ArrayList<>();
        for (SootMethod sm: sc.getMethods()) {
            if (sm.isConstructor()) continue;
            String nameLower = sm.getName().toLowerCase();
            // 根据函数名称排除一些 和可能是处理异常结果的回调函数
            if (nameLower.toLowerCase().contains("fail")) continue;
            if (nameLower.contains("except")) continue;
            if (nameLower.contains("error")) continue;
            res.add(sm);
        }
        return res;
    }


    public HashSet<Object> analyzePossibleCallbacks(SootClass sc) {
        HashSet<Object> callbackAnalysisInfo = new HashSet<>();
        boolean isHandlerType = false;
        if (handlerClass.isInterface() && sc.getInterfaces().contains(handlerClass)) {
            isHandlerType = true;
        } else if (!handlerClass.isInterface() && Scene.v().getActiveHierarchy().isClassSubclassOfIncluding(sc, handlerClass)) {
            isHandlerType = true;
        }
        if (isHandlerType) {
            SootMethod cb = locateStandardCallback(sc);
            if (cb != null) {
                // 可能会因为混淆等原因，找不到对应的回调函数
                HashMap<Integer, Value> paramIndex2Arg = new HashMap<>();
                for (int i = 0; i < cb.getParameterCount(); i ++) {
                    paramIndex2Arg.put(i, cb.retrieveActiveBody().getParameterLocal(i));
                }
                ForwardInvokeContext context = new ForwardInvokeContext(false, sc.getType(), callbackTaintedParamIndexes, paramIndex2Arg, true);
                ForwardTaintAnalysis fta = new ForwardTaintAnalysis(cb, context);
                callbackAnalysisInfo.addAll(fta.getImportantInformation());
            }
        } else {
            List<SootMethod> possibleCallbacks = locatePossibleCallbacks(sc);
            for (SootMethod cb: possibleCallbacks) {
                FlowSet<Integer> taintedParamIndexes = new ArraySparseSet<>();
                HashMap<Integer, Value> paramIndex2Arg = new HashMap<>();
                for (int i = 0; i < cb.getParameterCount(); i ++) {
                    Type paramType = cb.getParameterType(i);
                    if (paramType instanceof RefType) {
                        taintedParamIndexes.add(i);
                    }
                    paramIndex2Arg.put(i, cb.retrieveActiveBody().getParameterLocal(i));
                }

                ForwardInvokeContext context = new ForwardInvokeContext(false, sc.getType(), taintedParamIndexes, paramIndex2Arg, true);
                ForwardTaintAnalysis fta = new ForwardTaintAnalysis(cb, context);
                callbackAnalysisInfo.addAll(fta.getImportantInformation());
            }
        }
        return callbackAnalysisInfo;
    }

    public boolean isPossibleAbstractNetworkCallbackClass(SootClass sc) {
        /*
         网络回调函数接口：
         1.不应该是个幽灵类
         2.是接口或者抽象类
         3.回调函数应该至少有一个引用类型的参数
         */
        if (sc.isPhantomClass()) return false;
        if (!sc.isInterface() && !sc.isAbstract()) return false;
        boolean hasRefTypeParam = false;
        for (SootMethod sm: sc.getMethods()) {
            for (Type paramType: sm.getParameterTypes()) {
                if (paramType instanceof RefType) {
                    hasRefTypeParam = true;
                }
            }
        }
        return hasRefTypeParam;

    }
}
