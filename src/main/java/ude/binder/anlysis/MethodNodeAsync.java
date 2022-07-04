package ude.binder.anlysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodNodeAsync {
    private SootMethod method; // 当前节点表示的方法

    private boolean isInvokeObjectRequestRelated = false; // 表示调用对象是被request参数污染的对象
    private List<Integer> requestParamIndexes = new ArrayList<>(); // 如果这是一个包装函数，这里存储和url请求相关的参数索引
    private List<Integer> responseHandlerParamIndexes = new ArrayList<>(); // 如果这是一个包装函数，这里存储和response handler相关的参数索引；一般里面只有一个值，如果后面看到多个值，再另行处理
    // 需要一个结构，来保存上层传来的回调函数被调用的函数签名，以及被污染的参数
    // 反向分析的时候如果在某个方法节点里找到了具体的handler类型，那么就在该节点里调用该具体类型的回调函数，把这些共同的前向分析信息保存在该节点里

    private  List<MethodNodeAsync> predecessors = new ArrayList<>(); // 调用了method，且是一个包装函数的那些方法节点
    private MethodNodeAsync successor; // 后继节点，是被method调用的方法节点

    private Stmt invokeSuccessorStmt;
    private List<Edge> invokeEdges = new ArrayList<>(); // predecessors中调用了当前method的边，如果某个predecessor是一个wrapper，则不包含在其中



    public MethodNodeAsync(SootMethod sootMethod, Stmt stmt) {
        this.method = sootMethod;
        this.invokeSuccessorStmt = stmt;
    }

    public SootMethod getMethod() {
        return method;
    }

    public boolean isInvokeObjectRequestRelated() {
        return this.isInvokeObjectRequestRelated;
    }

    public void setInvokeObjectRequestRelated(boolean b) {
        this.isInvokeObjectRequestRelated = b;
    }

    public List<Integer> getRequestParamsIndexes() {
        return this.requestParamIndexes;
    }

    public List<Integer> getResponseHandlerParamsIndexes() { return this.responseHandlerParamIndexes; }

    public List<MethodNodeAsync> getPredecessors() {
        return this.predecessors;
    }

    public void addPredecessor(MethodNodeAsync wrapperMethodNode) {
        this.predecessors.add(wrapperMethodNode);
    }

    public void setSuccessor(MethodNodeAsync wrapperMethodNode) {
        this.successor = wrapperMethodNode;
    }

    public MethodNodeAsync getSuccessor() {
        return this.successor;
    }

    public void addInvokeEdge(Edge edge) {
        this.invokeEdges.add(edge);
    }

    public List<Edge> getInvokeEdges() {
        return this.invokeEdges;
    }

    private List<String> requestImportantInfo = new ArrayList<>(); // 当前方法节点里包含的信息
    public List<String> getLocalRequestImportantInfo() {
        return this.requestImportantInfo;
    }

    private HashSet<Object> responseImportantInfo = new HashSet<>(); // 当前方法节点里包含的信息
    public HashSet<Object> getLocalResponseImportantInfo() {
        return this.responseImportantInfo;
    }


    public Stmt getInvokeSuccessorStmt() { return invokeSuccessorStmt; }

    public void setInvokeSuccessorStmt(Stmt stmt) { this.invokeSuccessorStmt = stmt; }

    private boolean isWrapper; // 表示当前的node符合wrapper特征，即其拥有来自上层的请求和回调参数
    public boolean isWrapper() {
        return isWrapper;
    }

    public void setWrapper(boolean isWrapper) {
        this.isWrapper = isWrapper;
    }


    private boolean flag = false; // 表示当前的node被在某一条路径上被确定为一个wrapper

    public void setFlag(boolean isWrapper) {
        this.flag = isWrapper;
    }

    public boolean getFlag() {
        return this.flag;
    }
}
