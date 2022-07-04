package ude.binder;

import soot.*;
import soot.jimple.*;
import soot.toolkits.scalar.FlowSet;
import ude.AppAnalyzer;
import ude.backward.BackwardTaintAnalysis;
import ude.backward.InvokeAnalysisResult;
import ude.binder.anlysis.MethodNodeAsync;
import ude.binder.anlysis.MethodNodeSync;

import java.util.*;

public class Binder {

    protected void startBackwardAnalysis(SootMethod caller, Unit srcUnit, FlowSet<Value> sensitiveArgs, List<String> commonRequestInfo) {
        AppAnalyzer.addLogLine("[Backward]");

        if (commonRequestInfo != null) {
            for (String s : commonRequestInfo) {
                AppAnalyzer.addLogLine(s);
            }
        }

        List<BackwardTaintAnalysis> allBackWardTaintAnalysis = new ArrayList<>();
        new BackwardTaintAnalysis(caller, srcUnit, sensitiveArgs, new ArrayList<>(), allBackWardTaintAnalysis);
        List<String> allKeyInfos = mergeInvokeAnalysisResult(allBackWardTaintAnalysis);
        for (String s : allKeyInfos)
            AppAnalyzer.addLogLine(s);
        allBackWardTaintAnalysis.clear();
    }

    public static List<String> mergeInvokeAnalysisResult(List<BackwardTaintAnalysis> allBackwardTaintAnalysis) {
        HashSet<String> all = new HashSet<>();
        for (BackwardTaintAnalysis tmp : allBackwardTaintAnalysis) {
            HashMap<String, InvokeAnalysisResult> allInvokes = tmp.getAllInvokeAnalysisResults();
            for (Map.Entry<String, InvokeAnalysisResult> entry : allInvokes.entrySet()) {
                InvokeAnalysisResult invokeAnalysisResult = entry.getValue();
                if (invokeAnalysisResult != null) {
                    HashSet<String> keyInfos = invokeAnalysisResult.filterKeyInfo();
                    if (keyInfos.size() > 0)
                        all.addAll(keyInfos);
                }
            }
        }
        List<String> res = new ArrayList<>(all);
        res.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.charAt(0) - o2.charAt(0);
            }
        });
        return res;
    }


    protected void startForwardAnalysisFromCallback(MethodNodeAsync startNode) {

//        AppAnalyzer.addLogLine("[No Forward Analysis]");

        AppAnalyzer.addLogLine("[Forward]" + startNode.getMethod());
        Queue<MethodNodeAsync> queue = new LinkedList<>();
        queue.add(startNode);
        HashSet<Object> aggregatedRes = new HashSet<>();
        while (!queue.isEmpty()) {
            MethodNodeAsync wrapperNode = queue.remove();
            HashSet<Object> nodeRes = wrapperNode.getLocalResponseImportantInfo();
            aggregatedRes.addAll(nodeRes);

            List<MethodNodeAsync> predecessors = wrapperNode.getPredecessors();
            queue.addAll(predecessors);
        }

        extendFieldInfo(aggregatedRes);
        List<String> formattedRes = formatImportantInfo(aggregatedRes);
        for (String s : formattedRes) {
            AppAnalyzer.addLogLine(s);
        }
        AppAnalyzer.addLogLine("\n");
    }

    protected void startForwardAnalysisFromStmt(MethodNodeSync startNode) {
        AppAnalyzer.addLogLine("[Forward]" + startNode.getMethod());

        // 把上层的node信息全部合并进来
        Queue<MethodNodeSync> queue = new LinkedList<>();
        queue.add(startNode);
        HashSet<Object> aggregatedRes = new HashSet<>();
        while (!queue.isEmpty()) {
            MethodNodeSync wrapperNode = queue.remove();
            HashSet<Object> nodeRes = wrapperNode.getLocalResponseImportantInfo();
            aggregatedRes.addAll(nodeRes);

            List<MethodNodeSync> predecessors = wrapperNode.getPredecessors();
            queue.addAll(predecessors);
        }

        extendFieldInfo(aggregatedRes);
        List<String> formattedRes = formatImportantInfo(aggregatedRes);
        for (String s : formattedRes) {
            AppAnalyzer.addLogLine(s);
        }
        AppAnalyzer.addLogLine("\n");
    }

    public void extendFieldInfo(HashSet<Object> res) {
        HashSet<SootClass> allClasses = new HashSet<>();
        Queue<SootClass> queue = new LinkedList<>();
        for (Object item : res) {
            if (item instanceof SootClass) {
                if (!queue.contains(item)) {
                    queue.add((SootClass) item);
                }
            }
        }
        while (!queue.isEmpty()) {
            SootClass sc = queue.remove();
            if (!allClasses.contains(sc)) {
                allClasses.add(sc);
                for (SootField sf : sc.getFields()) {
                    Type tp = sf.getType();
                    if (tp instanceof RefType) {
                        SootClass fieldClass = ((RefType) tp).getSootClass();
                        if (fieldClass.isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(fieldClass)) {
                            if (!allClasses.contains(fieldClass) && !queue.contains(fieldClass)) {
                                queue.add(fieldClass);
                            }
                        }
                    }
                }
            }
        }
        AppAnalyzer.classesToRecordFields.addAll(allClasses);
//        for (SootClass sc : allClasses) {
//            for (SootField sf: sc.getFields()) {
//                res.add("ClassField:" + sf);
//            }
//        }
    }

    public List<String> formatImportantInfo(HashSet<Object> importantInfo) {
        List<String> res = new ArrayList<>();
        for (Object item : importantInfo) {
            if (item instanceof SootMethod) {
                res.add("Method:" + item);
            } else if (item instanceof SootClass) {
                res.add("Class:" + ((SootClass) item).getName());
            } else if (item instanceof StringConstant) {
                res.add("KeyString:" + ((StringConstant) item).value);
            } else if (item instanceof String) {
                res.add((String) item);
            }
        }
        return res;
    }

}
