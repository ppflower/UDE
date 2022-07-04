package ude.binder.anlysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodNodeSync {
    private SootMethod method; // 当前节点表示的方法

    private boolean isInvokeObjectRequestRelated = false; // 表示调用对象是被request参数污染的对象
    private List<Integer> requestParamIndexes = new ArrayList<>(); // 如果这是一个包装函数，这里存储和url请求相关的参数索引

    private  List<MethodNodeSync> predecessors = new ArrayList<>(); // 调用了method，且是一个包装函数的那些方法节点
    private MethodNodeSync successor; // 后继节点，是被method调用的方法节点
    private Stmt invokeSuccessorStmt;
    private List<Edge> invokeEdges = new ArrayList<>(); // predecessors中调用了当前method的边，如果某个predecessor是一个wrapper，则不包含在其中

    public MethodNodeSync(SootMethod sootMethod, Stmt stmt) {
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

    public List<MethodNodeSync> getPredecessors() {
        return this.predecessors;
    }

    public void addPredecessor(MethodNodeSync wrapperMethodNode) {
        this.predecessors.add(wrapperMethodNode);
    }

    public void setSuccessor(MethodNodeSync wrapperMethodNode) {
        this.successor = wrapperMethodNode;
    }

    public MethodNodeSync getSuccessor() {
        return this.successor;
    }

    public void addInvokeEdge(Edge edge) {
        this.invokeEdges.add(edge);
    }

    public List<Edge> getInvokeEdges() {
        return this.invokeEdges;
    }

    private HashSet<Object> requestImportantInfo = new HashSet<>(); // 当前方法节点里包含的信息
    public HashSet<Object> getLocalRequestImportantInfo() {
        return this.requestImportantInfo;
    }

    private HashSet<Object> responseImportantInfo = new HashSet<>(); // 当前方法节点里包含的信息
    public HashSet<Object> getLocalResponseImportantInfo() {
        return this.responseImportantInfo;
    }


    public Stmt getInvokeSuccessorStmt() { return invokeSuccessorStmt; }

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
