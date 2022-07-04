package ude;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AppFilter {
    public String androidJars;

    public AppFilter() {

    }

    public static void main(String[] args) throws Exception {

        AppFilter appFilter = new AppFilter();
        appFilter.analyzeRemote("/home/tendoyo/xuexiziliao/samples/ippps_apks_batch0/0/Social", false);
//        appAnalyzer.findOkhttp3InDir("failedTestCase", true);
//        appAnalyzer.findHttpUrlConnectionInDir("singleTestCase",true);
//        appAnalyzer.findHttpUrlConnectionInDir("/home/tendoyo/xuexiziliao/samples/ippps_apks_batch0/0/Social",false);
//        appAnalyzer.findHttpUrlConnectionInDir("/home/tendoyo/xuexiziliao/samples/ippps_apks_batch0/1/Social", false);


    }

    public void analyzeRemote(String dirPath, boolean isLocal) {
        if (isLocal) {
            androidJars = System.getenv("ANDROID_JARS");
        } else {
            androidJars = "/home/tendoyo/Android/SDK/platforms";
        }

        List<String> apkFiles = getApkFilesInDir(dirPath);
        for (String apkPath: apkFiles) {
            String apkName = new File(apkPath).getName();
            System.out.println("[Analyzing] " + apkName);
            String logPath = "volley_log/" + apkName.replace(".apk", ".txt");
            if (new File(logPath).exists()) continue;
            analyzeNetworkApis(apkPath);
        }
    }

    public void analyzeNetworkApis(String apkPath) {

        initSoot(apkPath);

        if (hasAsyncHttpClientUsage()) {
            log("hn/library_usage/AsyncHttpClient.txt", apkPath);
        }
    }

    public boolean hasAsyncHttpClientUsage() {
        SootClass volleyClass = Scene.v().getSootClass("com.loopj.android.http.AsyncHttpClient");
        if (volleyClass.isApplicationClass()) {
            return true;
        }
        return false;
    }

    public void findVolleyInDir(String dirPath, boolean isLocal) {
        if (isLocal) {
            androidJars = System.getenv("ANDROID_JARS");
        } else {
            androidJars = "/home/tendoyo/Android/SDK/platforms";
        }

        String logDirPath = "volley_log/";
        File logDir = new File(logDirPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        List<String> apkFiles = getApkFilesInDir(dirPath);
        for (String apkPath: apkFiles) {
            String apkName = new File(apkPath).getName();
            System.out.println("[Analyzing] " + apkName);
            String logPath = "volley_log/" + apkName.replace(".apk", ".txt");
            if (new File(logPath).exists()) continue;
            findVolleyCallbacks(apkPath, logPath);
        }
    }

    public void findOkhttp3InDir(String dirPath, boolean isLocal) {
        if (isLocal) {
            androidJars = System.getenv("ANDROID_JARS");
        } else {
            androidJars = "/home/tendoyo/Android/SDK/platforms";
        }

        String logDirPath = "okhttp3_log/";
        File logDir = new File(logDirPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        List<String> apkFiles = getApkFilesInDir(dirPath);
        for (String apkPath: apkFiles) {
            String apkName = new File(apkPath).getName();
            System.out.println("[Analyzing] " + apkName);
            String logPath = logDirPath + apkName.replace(".apk", ".txt");
            if (new File(logPath).exists()) continue;
            findOkhttp3Callbacks(apkPath, logPath);
        }
        System.exit(0);
    }

    public void findHttpClientInDir(String dirPath, boolean isLocal) {
        if (isLocal) {
            androidJars = System.getenv("ANDROID_JARS");
        } else {
            androidJars = "/home/tendoyo/Android/SDK/platforms";
        }

        String logDirPath = "httpclient_log/";
        File logDir = new File(logDirPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        List<String> apkFiles = getApkFilesInDir(dirPath);
        for (String apkPath: apkFiles) {
            String apkName = new File(apkPath).getName();
            System.out.println("[Analyzing] " + apkName);
            String logPath = logDirPath + apkName.replace(".apk", ".txt");
            if (new File(logPath).exists()) continue;
            findHttpClientCallbacks(apkPath, logPath);
        }
        if (isLocal) {
            System.exit(0);
        }
    }

    public void findHttpUrlConnectionInDir(String dirPath, boolean isLocal) {
        if (isLocal) {
            androidJars = System.getenv("ANDROID_JARS");
        } else {
            androidJars = "/home/tendoyo/Android/SDK/platforms";
        }

        String logDirPath = "httpUrlConnection_log/";
        File logDir = new File(logDirPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        List<String> apkFiles = getApkFilesInDir(dirPath);
        for (String apkPath: apkFiles) {
            String apkName = new File(apkPath).getName();
            System.out.println("[Analyzing] " + apkName);
            String logPath = logDirPath + apkName.replace(".apk", ".txt");
            if (new File(logPath).exists()) continue;
            findHttpUrlConnectionCall(apkPath, logPath);
        }
        if (isLocal) System.exit(0);
    }

    public void initFlowDroid(String apkPath) {
        InfoflowAndroidConfiguration configuration = new InfoflowAndroidConfiguration();
        configuration.setDataFlowTimeout(180);
        configuration.getCallbackConfig().setCallbackAnalysisTimeout(180);
        configuration.setCallgraphAlgorithm(InfoflowAndroidConfiguration.CallgraphAlgorithm.CHA);
        configuration.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        configuration.getAnalysisFileConfig().setAndroidPlatformDir(androidJars);
        configuration.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        configuration.setMergeDexFiles(true);

        SetupApplication app = new SetupApplication(configuration);
        app.constructCallgraph();

    }

    public void initSoot(String apkPath) {
        G.reset();
        String ANDROID_JARS = System.getenv("ANDROID_JARS");

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_keep_offset(false);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_android_jars(ANDROID_JARS);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(apkPath));

        Scene.v().loadNecessaryClasses();
    }

    public void findVolleyCallbacks(String apkPath, String logPath) {
        initFlowDroid(apkPath);
        CallGraph callGraph = Scene.v().getCallGraph();
        Hierarchy hierarchy = new Hierarchy();

        SootClass clsListener = Scene.v().getSootClass("com.android.volley.Response$Listener");
        List<SootClass> listenerImpls = hierarchy.getImplementersOf(clsListener);
        for (SootClass listenerImpl: listenerImpls) {
            if (listenerImpl.getName().startsWith("com.android.volley.")) continue;

            SootMethod constructor = listenerImpl.getMethodByName("<init>");
            if (constructor != null) {
                Iterator<Edge> edgeIterator = callGraph.edgesInto(constructor);
                while (edgeIterator.hasNext()) {
                    Edge edge = edgeIterator.next();
                    SootMethod srcMethod = edge.src();
                    log(logPath, "[Invoke] " + srcMethod + " ===> " + constructor);
                }
            } else {
                System.err.println(listenerImpl + " has no constructor!");
            }
        }
    }

    public void findOkhttp3Callbacks(String apkPath, String logPath) {
        initSoot(apkPath);
        SootClass callback = Scene.v().getSootClass("okhttp3.Callback");
        if (callback.isPhantomClass()) {
            System.out.println("okhttp3.Callback is phantom");
            return;
        }

        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        List<SootClass> callbackImpls = hierarchy.getDirectImplementersOf(callback);
        for (SootClass callbackImpl: callbackImpls) {
            log(logPath, callbackImpl.getName());
        }
    }

    public void findHttpClientCallbacks(String apkPath, String logPath) {
        initSoot(apkPath);
        SootClass callbackCls = Scene.v().getSootClass("com.loopj.android.http.ResponseHandlerInterface");
        if (callbackCls.isPhantomClass()) {
            System.out.println("com.loopj.android.http.AsyncHttpResponseHandler is phantom");
            return;
        }

        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        List<SootClass> callbackImpls = hierarchy.getImplementersOf(callbackCls);
        for (SootClass callbackImpl: callbackImpls) {
            log(logPath, callbackImpl.getName());
        }
    }

    public void findHttpUrlConnectionCall(String apkPath, String logPath) {
        initSoot(apkPath);

        for (SootClass sootClass: Scene.v().getApplicationClasses()) {
            if (AppAnalyzer.is3rdPartyLibrary(sootClass)) continue;

            List<SootMethod> declaredMethods = new ArrayList<>(sootClass.getMethods()); // retrieveActiveBody方法有可能会把方法提中用到的父类的方法添加到当前的类中，会导致遍历方法的时候产异常（类的方法列表发生变化）

            for (SootMethod sootMethod: declaredMethods) {
                if (!sootMethod.isConcrete()) continue;
                UnitPatchingChain units;
                Body body = sootMethod.retrieveActiveBody();
                units = body.getUnits();
//                try {
//                    Body body = sootMethod.retrieveActiveBody();
//                    units = body.getUnits();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    continue;
//                }

                for (Unit unit: units) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethodRef methodRef = invokeExpr.getMethodRef();
                        if (methodRef.getSignature().equals("<java.net.URL: java.net.URLConnection openConnection()>")) {
//                            log(logPath, sootMethod.getSignature());
                        }
                    }
                }
            }
        }
    }





















    public void log(String logPath, String content) {
        addLine(logPath, content);
    }

    public void addLine(String filePath, String content) {
        BufferedWriter out = null ;
        try {
            out = new BufferedWriter( new OutputStreamWriter(
                    new FileOutputStream(filePath, true )));
            out.write(content + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();
        //读取文件至 words 字符串数组中
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filePath)));
            String linestr;//按行读取 将每次读取一行的结果赋值给linestr
            while ((linestr = br.readLine()) != null) {
                lines.add(linestr);//赋值给数组后，下标后移
            }
            br.close();//关闭IO
        } catch (Exception e) {
            System.out.println("文件操作失败");
            e.printStackTrace();
        }
        return lines;
    }

    public List<String> getApkFilesInDir(String dirPath) {
        File dir = new File(dirPath);
        if (dir.isFile()) {
            return null;
        }
        List<String> apkFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        for (File file: files) {
            if (file.isFile() && file.getName().endsWith(".apk")) {
                apkFiles.add(file.getAbsolutePath());
            }
        }
        return apkFiles;
    }

}
