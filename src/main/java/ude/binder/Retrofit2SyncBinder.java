package ude.binder;

import soot.*;
import soot.jimple.ClassConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.*;
import ude.AppAnalyzer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Retrofit2SyncBinder extends Binder {
    // 同步和异步放在一起处理

    public boolean checkAndBind(SootMethod invokeContextMethod, Stmt apiInvokeStmt) {
        InvokeExpr invokeExpr = apiInvokeStmt.getInvokeExpr();
        SootMethod sm = invokeExpr.getMethod();
        if (sm.getDeclaringClass().isApplicationClass() && !AppAnalyzer.is3rdPartyLibrary(sm.getDeclaringClass())) {
            if (isRetrofit2RequestMethod(sm)) {
                HashSet<Object> requestInfo = new HashSet<>();
                HashSet<Object> responseInfo = new HashSet<>();
                getRequestAndResponseInfo(sm, requestInfo, responseInfo);

//                if (!requestInfo.isEmpty() && !responseInfo.isEmpty()) {}
                AppAnalyzer.addLogLine("[Backward]");
                extendFieldInfo(requestInfo);
                List<String> formattedInfo = formatImportantInfo(requestInfo);
                for (String s : formattedInfo) {
                    AppAnalyzer.addLogLine(s);
                }

                AppAnalyzer.addLogLine("[Forward]" + invokeContextMethod);
                extendFieldInfo(responseInfo);
                List<String> formattedRes = formatImportantInfo(responseInfo);
                for (String s : formattedRes)
                    AppAnalyzer.addLogLine(s);
                AppAnalyzer.addLogLine("\n");

                // --------------------------------------------------
                List<Integer> requestIndexes = new ArrayList<>();
                for (int index=0; index < invokeExpr.getArgCount(); index ++) {
                    requestIndexes.add(index);
                }
                new SyncBinder().findSyncWrappersAndAnalyze(invokeContextMethod, apiInvokeStmt, requestIndexes, false);
                // 追踪retrofit方法的数据流，可以补充一些有语义的信息

                return true;
            }
        }

        return false;
    }

    public boolean isRetrofit2RequestMethod(SootMethod sm) {
        // 准确性可以进一步提高，增加一个分析，判断其声明类是否经过Retrofit.create方法

        if (!sm.getDeclaringClass().isInterface()) {
            return false;
        }

//        Type returnType = sm.getReturnType();
//        if (returnType instanceof RefType) {
//            RefType returnRefType = (RefType) returnType;
//            if (returnRefType.getSootClass() == Scene.v().getSootClass("retrofit2.Call")) {
//                return true;
//            }
//        }

        for (Tag eachTag : sm.getTags()) {
            if (eachTag.toString().contains("retrofit"))
                return true;
        }
        return false;
    }

    public void getRequestAndResponseInfo(SootMethod sm, HashSet<Object> requestInfo, HashSet<Object> responseInfo) {
        for (Tag tag: sm.getTags()) {
            // 不考虑path了
//            if (tag instanceof VisibilityAnnotationTag) {
//                // 请求的url path
//                List<AnnotationTag> annotationTags = ((VisibilityAnnotationTag) tag).getAnnotations();
//                for (AnnotationTag annotationTag: annotationTags) {
//                    String annoType = annotationTag.getType();
//                    if ("Lretrofit2/http/POST;".equals(annoType)
//                            || "Lretrofit2/http/GET;".equals(annoType)) {
//                        for (AnnotationElem annotationElem: annotationTag.getElems()) {
//                            if (annotationElem instanceof AnnotationStringElem) {
//                                String path = ((AnnotationStringElem) annotationElem).getValue();
//                                requestInfo.add("String:" + path);
//                            }
//                        }
//                    }
//                }
//            } else
            if (tag instanceof VisibilityParameterAnnotationTag) {
                // 请求的参数，可能带语义
                List<VisibilityAnnotationTag> visibilityAnnotationTags = ((VisibilityParameterAnnotationTag) tag).getVisibilityAnnotations();
                for (VisibilityAnnotationTag visibilityAnnotationTag: visibilityAnnotationTags) {
                    if (visibilityAnnotationTag == null) continue;
                    for (AnnotationTag annotationTag: visibilityAnnotationTag.getAnnotations()) {
                        for (AnnotationElem annotationElem: annotationTag.getElems()) {
                            if (annotationElem instanceof AnnotationStringElem) {
                                String param = ((AnnotationStringElem) annotationElem).getValue();
                                requestInfo.add("String:" + param);
                            }
                        }
                    }
                }
            }
            else if (tag instanceof SignatureTag) {
                String signature = ((SignatureTag) tag).getSignature();
                // 常见的为Lretrofit2/Call<xxx>; 这里尽量也支持一些其他形式，包括混淆
                String returnPartSig = signature.substring(signature.indexOf(")")  + 1);
                String returnPartClsPath = parseGenericType(returnPartSig);
                if (returnPartClsPath != null) {
                    try {
                        RefType respType = (RefType) ClassConstant.v(returnPartClsPath).toSootType();
                        SootClass respClass = respType.getSootClass();
                        responseInfo.add(respClass); // add important class info
//                        System.out.println("    " + respClass);
                    } catch (Exception e) {
                        System.err.println("    Fail to parse type " + returnPartClsPath + " in retrofit method " + sm);
                    }
                }
            }
        }

    }

    public String parseGenericType(String typePathWithGenerics) {
        int startIndex = typePathWithGenerics.lastIndexOf("<")+1;
        int endIndex = typePathWithGenerics.indexOf(">");
        if (startIndex > 0 && endIndex > 0) {
            return typePathWithGenerics.substring(startIndex, endIndex);
        }
        return null;
    }
}
