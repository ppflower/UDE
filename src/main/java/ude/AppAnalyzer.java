package ude;

import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import ude.binder.*;
import ude.binder.anlysis.MustAliasAnalysis;
import ude.binder.anlysis.ParamPassAnalysis;
import ude.binder.anlysis.ParamTaintAnalysis;
import ude.forward.ForwardTaintAnalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class AppAnalyzer {

    public static String androidJars;
    public static List<String> thirdPartyLibraryPrefixList;

    public static List<String> backwardFilterPrefixList;

    public static long startTime;
    public static long endTime;

    public static String appId;
    public static String logFilePath;
    public static File logFile;
    public static String classFieldInfoFilePath;
    public static File classFieldInfoFile;

    FastAndroidNetworkingBinder fastAndroidNetworkingBinder;
    OkHttp3SyncBinder okHttp3SyncBinder;
    OkHttp3AsyncBinder okHttp3AsyncBinder;
    VolleyBinder volleyBinder;
    AndroidAsyncHttpClientBinder androidAsyncHttpClientBinder;
    HttpUrlConnectionBinder httpUrlConnectionBinder;
    HttpClientV5SyncBinder httpClientV5SyncBinder;
    HttpClientV5AsyncBinder httpClientV5AsyncBinder;
    HttpClientV4SyncBinder httpClientV4SyncBinder;
    HttpClientV4AsyncBinder httpClientV4AsyncBinder;
    Retrofit2SyncBinder retrofit2SyncBinder;

    static {
        androidJars = System.getenv("ANDROID_JARS");
        thirdPartyLibraryPrefixList = FileTool.readLinesFromFile("apis/libraries.txt");
        backwardFilterPrefixList = FileTool.readLinesFromFile("apis/backwardFilter.txt");
//        isDebugging = true;

    }

    public static HashMap<SootMethod, MustAliasAnalysis> analyzedAliases;

    public static List<String> importantInformation;
    public static HashSet<SootClass> classesToRecordFields;

    public static SootMethod startActivityMethod;
    public static SootMethod newIntentMethod;

    public AppAnalyzer() {
    }

    public static void main(String[] args) {

//        appAnalyzer.analyze(2, "/Users/flower/AndroidStudioProjects/TestApp/app/build/outputs/apk/debug/app-debug.apk"); // test thread
//        System.exit(0);

//        appAnalyzer.analyze(0, "/Users/flower/IdeaProjects/SootLabs/testCases/cam.light.android.apk"); // volley
//        appAnalyzer.analyze(1, "/Users/flower/IdeaProjects/SootLabs/testCases/com.axiaodiao.melo.apk"); // android async http client
//        appAnalyzer.analyze(2, "/Users/flower/IdeaProjects/SootLabs/testCases/com.analysisplus.app.apk"); // android async http client case不太好
//        appAnalyzer.analyze(3, "/Users/flower/IdeaProjects/SootLabs/testCases/ru.taboo.app.apk"); // okhttp3 firebase sync yyt检查
//        appAnalyzer.analyze(4, "/Users/flower/Downloads/com.mooq.dating.chat.apk"); // retrofit2
//        appAnalyzer.analyze(5, "/Users/flower/Downloads/br.com.promobit.app.apk"); // retrofit2
//        appAnalyzer.analyze(6, "/Users/flower/Downloads/com.hibobi.store.apk"); // retrofit2 有些奇怪的机制，并不是retrofit的通常用法，没有理解
//        appAnalyzer.analyze(7, "/Users/flower/Downloads/com.panaceasoft.troquei.apk"); // retrofit2 没有扫出来，用于判断retrofit调用的地方全部被混淆
//        appAnalyzer.analyze(8, "/Users/flower/Downloads/com.cupichat.android.apk"); // 已经发现了使用http url connection 其他的使用方式暂未发现。
//        appAnalyzer.analyze(9, "/Users/flower/Downloads/com.likemeet.apk"); // retrofit2
//        appAnalyzer.analyze(10, "/Users/flower/Downloads/emotion.onekm.apk"); // okhttp enqueue

//        appAnalyzer.analyze(11, "/Users/flower/Downloads/com.sugardaddydating.sugardaddy.sugardaddy.apk"); // okhttp enqueue

//        appAnalyzer.analyze(12, "/Users/flower/Downloads/com.rayandating.singleDoctors.apk"); // fast android networking

//        appAnalyzer.analyze(13, "/Users/flower/Downloads/tmp_apks/com.cherrygroup.dating.apk"); // volley 封装
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.videochat.alo.apk"); // volley 封装



//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.weblinkstech.bebolive.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/ru.taboo.app.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.rayandating.divorcedSingles.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.panaceasoft.troquei.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.hibobi.store.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.kuzgun.giftplay.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/tmp_apks/com.cupichat.android.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.quack.app.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.serenat.videochat.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/messenger.video.call.chat.free.apk");

//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.likemeet.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.weblinkstech.bebolive.apk");



//        appAnalyzer.analyze(14, "/Users/flower/Downloads/br.com.promobit.app.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/ly.omegle.android.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.weblinkstech.bebolive.apk");

//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.mooq.dating.chat.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.likemeet.apk");
//
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.cupichat.android.apk");
//        appAnalyzer.analyze(14, "/Users/flower/Downloads/com.probits.argo.apk"); // android async http client
//        System.exit(0);




        //----------------------------
        runOnDataset();
    }

    public static void runOnDataset() {
        AppAnalyzer appAnalyzer = new AppAnalyzer();

        File logDir = new File("ude_logs");
        if (!logDir.exists()) logDir.mkdir();

        startTime = System.currentTimeMillis();

        List<String> categoryDirPaths = getFilePathsInDirPath("/home/tendoyo/xuexiziliao/hqd/download_apks", false, true);
        for (String categoryDirPath: categoryDirPaths) {
            String category = new File(categoryDirPath).getName();
            File categoryFlagFile = new File("cateFlags/" + category);
            if (categoryFlagFile.exists()) continue;
            try {
                categoryFlagFile.createNewFile();
            } catch (Exception e) {
                System.err.println("Fail to create flag file.");
                System.exit(0);
            }

            long cateStartTime = System.currentTimeMillis();

            List<String> apkPaths = getFilePathsInDirPath(categoryDirPath, true, false);
            System.out.println(categoryDirPath);
            System.out.println(apkPaths.size());
            for (int i = 0; i < apkPaths.size(); i ++) {
                if (apkPaths.get(i).endsWith(".apk")) {
                    appAnalyzer.limitTaskTime(i, apkPaths.get(i), 10);
                }
            }

            long cateEndTime = System.currentTimeMillis();
            FileTool.addLine("hn/time_record.txt", category + ": " + (cateEndTime-cateStartTime)/1000 + "s");
        }

        endTime = System.currentTimeMillis();
        System.out.println("耗时: " + ((endTime-startTime)/1000) + "秒");
    }

    public static void runOnGroundTruth() {
        AppAnalyzer appAnalyzer = new AppAnalyzer();

        File logDir = new File("ude_logs");
        if (!logDir.exists()) logDir.mkdir();

        List<String> apkPaths = getTestApps();

        System.out.println("Total: " + apkPaths.size());

        int batch = newBatch();
        System.out.println("Batch " + batch);

        startTime = System.currentTimeMillis();
        int countEachBatch = 15; // 每个batch分析的apk数量
        for (int i = batch * countEachBatch; i < (batch + 1) * countEachBatch; i++) {
            if (i >= apkPaths.size()) break;
            if (apkPaths.get(i).endsWith(".apk")) {
                appAnalyzer.limitTaskTime(i, apkPaths.get(i), 10);
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("耗时: " + ((endTime-startTime)/1000) + "秒");
    }

    public static List<String> getTestApps() {
        // 获取所有test case的路径
        String[] testAppIds = new String[]{
                "ru.taboo.app.apk",
                "com.weblinkstech.bebolive.apk",
                "com.rayandating.divorcedSingles.apk",
                "com.mooq.dating.chat.apk",
                "com.cupichat.android.apk",
                "com.rayandating.seriousRelationship.apk",
                "com.likemeet.apk",
                "emotion.onekm.apk",
                "com.rayandating.singleDoctors.apk",
                "com.karima.dating.apk",
                "com.rayandating.seniorSingle.apk",
                "com.rayandating.lastinlove.apk",
                "com.rayandating.muslimDating.apk",
                "com.rayandating.eliteSingles.apk",
                "com.rayandating.euroDating.apk",
                "com.plusde50karima.dating.apk",
                "com.rayandating.singleParents.apk",
                "com.rayandating.internationalDating.apk",
                "com.rayandating.christianDating.apk",
                "com.rayandating.tattooedSingles.apk",
                "br.com.promobit.app.apk",
                "com.banggood.client.apk",
                "com.hibobi.store.apk",
                "com.panaceasoft.troquei.apk",
                "com.hotmart.sparkle.apk",
                "com.quack.app.apk",
                "br.com.conselheiros.android.apk",
                "com.letterboxd.letterboxd.apk",
                "com.zerophil.worldtalk.apk",
                "com.kuzgun.giftplay.apk",
                "com.poqe.android.apk",
                "messenger.video.call.chat.free.apk",
                "com.serenat.videochat.apk",
                "com.realu.dating.apk",
                "ly.omegle.android.apk",
                "com.waplogmatch.social.apk",
                "com.jaumo.apk",
                "omegle.tv.apk",
                "com.asiainno.uplive.apk",
                "com.lomotif.android.apk",
                "com.huanliao.tiya.apk",
                "com.textmeinc.textme.apk",
                "com.clubhouse.app.apk",
                "com.mi.global.bbs.apk",
                "com.superlive.liveapp.apk"
        };
        List<String> apkPaths = new ArrayList<>();
        File apkRootDir = new File("/home/tendoyo/xuexiziliao/hqd/download_apks");
        File[] files = apkRootDir.listFiles();

        for (File file : files) {
            if (!file.isDirectory()) continue;
            File categoryDir = file;
            File[] apkFiles = categoryDir.listFiles();
            for (File apkFile : apkFiles) {
                String apkFileName = apkFile.getName();
                for (String testAppId: testAppIds) {
                    if (testAppId.equals(apkFileName))
                        apkPaths.add(apkFile.getAbsolutePath());
                }
            }
        }
        return apkPaths;
    }

    public static List<String> getAllApps() {
        // 获取要分析的apk路径
        List<String> apkPaths = new ArrayList<>();
        File apkRootDir = new File("/home/tendoyo/xuexiziliao/hqd/download_apks");
        File[] files = apkRootDir.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) continue;
            File categoryDir = file;
            File[] apkFiles = categoryDir.listFiles();
//            for (int i = 0; i < 50; i ++) {
//                apkPaths.add(apkFiles[i].getAbsolutePath());
//            }
            for (File apkFile : apkFiles) {
                apkPaths.add(apkFile.getAbsolutePath());
            }
        }
        return apkPaths;
    }

    public static List<String> getFilePathsInDirPath(String dirPath, boolean onlyFile, boolean onlyDir) {
        List<String> res = new ArrayList<>();
        File dirFile = new File(dirPath);
        File[] files = dirFile.listFiles();
        for (File file : files) {
            if (file.isFile() && onlyDir) continue;
            if (file.isDirectory() && onlyFile) continue;

            String filePath = file.getAbsolutePath();
            res.add(filePath);

            if (onlyDir) {
                // 稍微做一下排序，优先分析关注的类
                if (file.getName().toLowerCase().contains("dating")
                        || file.getName().toLowerCase().contains("social")
                        || file.getName().toLowerCase().contains("communication")
                        || file.getName().toLowerCase().contains("shopping")) {
                    res.remove(filePath);
                    res.add(0, filePath);
                }
            }

        }
        return res;
    }

    public void limitTaskTime(int i, String apkPath, int timeOut) {
        // 获取线程池
        ExecutorService es = Executors.newFixedThreadPool(1);

        // Future用于执行多线程的执行结果
        Future<Boolean> future = es.submit(() -> {
            analyze(i, apkPath);
            return true;
        });

        try {
            // future.get()测试被执行的程序是否能在timeOut时限内返回字符串
            future.get(timeOut, TimeUnit.MINUTES);
            return;
        } catch (Exception ex) {
            System.out.println("线程错误：" + ex.getMessage());
        } finally {
            // 关闭线程池
            es.shutdown();
        }

        AppAnalyzer.logFile.delete();
        AppAnalyzer.classFieldInfoFile.delete();
    }

    public void analyze(int index, String apkPath) {
        // Prepare

        boolean successful = initFilePaths(apkPath); // 初始化log文件，如果log文件已经存在就跳过这个app
        if (!successful) return;

        System.out.println("Analyzing " + (index % 500) + " " + AppAnalyzer.logFile.getParentFile().getName() + "/" + AppAnalyzer.logFile.getName());

        try {
            initAnalysisData(apkPath); // 初始化所有信息，包括apk的内容、分析单个apk要用到的数据结构等等

            analyzeUnits(); // 逐条分析apk内的语句

            recordImportantInfo(); // 将报出来的关键信息保存到文件
            recordClassFieldInfo(); // 将报出来的信息相关类的field保存到文件
        } catch (Throwable throwable) {
            throwable.printStackTrace();

            System.out.println("\n[Fail] to analyze " + AppAnalyzer.logFile.getParentFile().getName() + "/" + AppAnalyzer.logFile.getName() + "\n");
            AppAnalyzer.logFile.delete();
            AppAnalyzer.classFieldInfoFile.delete();
        }
    }

    public void analyzeUnits() {
        // 对apk代码的每一条语句分析，看看其是否是网络api的调用
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (AppAnalyzer.is3rdPartyLibrary(sootClass)) continue;
            List<SootMethod> declaredMethods = new ArrayList<>(sootClass.getMethods());
            for (SootMethod sootMethod : declaredMethods) {
                if (!sootMethod.isConcrete()) continue;

                UnitPatchingChain units = sootMethod.retrieveActiveBody().getUnits();
                for (Unit unit : units) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        handleInvoke(sootMethod, stmt);
                    }
                }
            }
        }
    }


    public void handleInvoke(SootMethod sootMethod, Stmt stmt) {
        boolean binded = false;
        if (!binded) binded = fastAndroidNetworkingBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = okHttp3AsyncBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = okHttp3SyncBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = volleyBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = androidAsyncHttpClientBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = httpUrlConnectionBinder.checkAndBind(sootMethod, stmt);
        if (!binded)
            binded = retrofit2SyncBinder.checkAndBind(sootMethod, stmt);

        if (!binded)
            binded = httpClientV5SyncBinder.checkAndBind(sootMethod, stmt);
        if (binded)
            FileTool.addLine("hn/HttpClientV5Sync.txt", AppAnalyzer.logFilePath);
        if (!binded)
            binded = httpClientV5AsyncBinder.checkAndBind(sootMethod, stmt);
        if (binded)
            FileTool.addLine("hn/HttpClientV5Async.txt", AppAnalyzer.logFilePath);
        if (!binded)
            binded = httpClientV4SyncBinder.checkAndBind(sootMethod, stmt);
        if (binded)
            FileTool.addLine("hn/HttpClientV4Sync.txt", AppAnalyzer.logFilePath);
        if (!binded)
            binded = httpClientV4AsyncBinder.checkAndBind(sootMethod, stmt);
        if (binded)
            FileTool.addLine("hn/HttpClientV4Async.txt", AppAnalyzer.logFilePath);
    }

    public boolean initFilePaths(String apkPath) {
        // 如果成功创建了一个文件返回true，否则返回false（文件存在、创建失败）
        File apkFile = new File(apkPath);
        String apkName = apkFile.getName();
        String categoryName = apkFile.getParentFile().getName();
        String logCategoryDirPath = "ude_logs/" + categoryName + "/";
        File logCategoryDir = new File(logCategoryDirPath);
        if (!logCategoryDir.exists()) logCategoryDir.mkdir();
        AppAnalyzer.appId = apkName.substring(0, apkName.length() - 4);
        AppAnalyzer.logFilePath = logCategoryDirPath + AppAnalyzer.appId + ".txt";
        AppAnalyzer.classFieldInfoFilePath = logCategoryDirPath + AppAnalyzer.appId + "_field_info.txt";
        AppAnalyzer.logFile = new File(AppAnalyzer.logFilePath);
        AppAnalyzer.classFieldInfoFile = new File(AppAnalyzer.classFieldInfoFilePath);
        if (logFile.exists()) {
            return false;
        } else {
            try {
                AppAnalyzer.logFile.createNewFile();
                AppAnalyzer.classFieldInfoFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void initAnalysisData(String apkPath) {
        initFlowDroid(apkPath);

        AppAnalyzer.analyzedAliases = new HashMap<>();
        AppAnalyzer.importantInformation = new ArrayList<>();
        AppAnalyzer.classesToRecordFields = new HashSet<>();
        AppAnalyzer.startActivityMethod = Scene.v().getMethod("<android.app.Activity: void startActivity(android.content.Intent)>");
        AppAnalyzer.newIntentMethod = Scene.v().getMethod("<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>");

        ForwardTaintAnalysis.initConsts();
        ParamPassAnalysis.initConsts();
        ParamTaintAnalysis.initConsts();


        fastAndroidNetworkingBinder = new FastAndroidNetworkingBinder();
        okHttp3SyncBinder = new OkHttp3SyncBinder();
        okHttp3AsyncBinder = new OkHttp3AsyncBinder();
        volleyBinder = new VolleyBinder();
        androidAsyncHttpClientBinder = new AndroidAsyncHttpClientBinder();
        httpUrlConnectionBinder = new HttpUrlConnectionBinder();
        httpClientV5SyncBinder = new HttpClientV5SyncBinder();
        httpClientV5AsyncBinder = new HttpClientV5AsyncBinder();
        httpClientV4SyncBinder = new HttpClientV4SyncBinder();
        httpClientV4AsyncBinder = new HttpClientV4AsyncBinder();
        retrofit2SyncBinder = new Retrofit2SyncBinder();
    }

    public void recordImportantInfo() {
        FileTool.addLines(logFilePath, importantInformation);
    }
    public void recordClassFieldInfo() {
        List<String> lines = new ArrayList<>();
        for (SootClass sc : classesToRecordFields) {
            for (SootField sf : sc.getFields()) {
                lines.add("ClassField:" + sf);
            }
        }
        FileTool.addLines(classFieldInfoFilePath, lines);
    }

    public static boolean isSubclassOrImplementer(SootClass possibleSubclassOrImplementer, SootClass sc) {
        if (!sc.isInterface()) {
            return possibleSubclassOrImplementer.getSuperclassUnsafe() == sc;
        } else {
            return possibleSubclassOrImplementer.implementsInterface(sc.getName());
        }
    }
    public static boolean is3rdPartyLibrary(SootClass sootClass) {
        String clsName = sootClass.getName();
        if (clsName.startsWith(AppAnalyzer.appId)) {
            return false;
        }
        for (String thirdPartyLibraryPrefix : thirdPartyLibraryPrefixList) {
            if (clsName.startsWith(thirdPartyLibraryPrefix)) {
                return true;
            }
        }
        return false;
    }
    public static void addLogLine(String line) {
        importantInformation.add(line);
    }


    public static SootMethod locateMethod(SootClass sootClass, String subSignature) {
        SootClass currentClass = sootClass;
        do {
            SootMethod sm = currentClass.getMethodUnsafe(subSignature);
            if (sm != null) return sm;

            if (currentClass.hasSuperclass()) {
                currentClass = currentClass.getSuperclass();
            } else {
                break;
            }
        } while (true);

        System.err.println("Cannot find method in " + sootClass);
        return null;
    }

    public static int newBatch() {
        int batch = 0;
        while (true) {
            File file = new File("batch/" + batch + ".txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            batch++;
        }
        return batch;
    }



    public void initFlowDroid(String apkPath) {
        InfoflowAndroidConfiguration configuration = new InfoflowAndroidConfiguration();
        configuration.setDataFlowTimeout(180);
        configuration.getCallbackConfig().setCallbackAnalysisTimeout(120);
        configuration.setCallgraphAlgorithm(InfoflowAndroidConfiguration.CallgraphAlgorithm.CHA);
        configuration.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        configuration.getAnalysisFileConfig().setAndroidPlatformDir(androidJars);
        configuration.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        configuration.setMergeDexFiles(true);

        SetupApplication app = new SetupApplication(configuration);
        app.constructCallgraph();
    }














    public static boolean isAnonymousConstructor(SootMethod sootMethod) {
        String pattern = ".+\\$\\d+$";
        String clsName = sootMethod.getDeclaringClass().getName();
        return sootMethod.getName().equals("<init>") && Pattern.matches(pattern, clsName);
    }
}
