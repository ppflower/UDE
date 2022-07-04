package ude.backward;

import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class InvokeAnalysisResult {
    private SootMethod sootMethod;
    private List<String> callChain;
    private List<Unit> riskUnits;
    private List<String> analysisDetails;

    private List<String> riskInfo;

    public InvokeAnalysisResult(SootMethod sm, List<String> callChain) {
        this.sootMethod = sm;
        this.callChain = callChain;
        this.riskUnits = new ArrayList<>();
        this.analysisDetails = new ArrayList<>();
        this.riskInfo = new ArrayList<>();
    }


    public void addRiskUnits(List<Unit> units) {
        this.riskUnits.addAll(units);
    }

    public void addAnalysisDetails(List<String> analysisDetails) {
        this.analysisDetails.addAll(analysisDetails);
    }

    public void addRiskInfo(List<String> riskInfo) {
        this.riskInfo.addAll(riskInfo);
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public List<Unit> getRiskUnits() {
        return riskUnits;
    }

    public List<String> getRiskInfo() {
        return this.riskInfo;
    }

    public List<String> getAnalysisDetails() {
        return analysisDetails;
    }

    public List<String> getCallChain() {
        return callChain;
    }

    public HashSet<String> filterKeyInfo() {
        HashSet<String> res = new HashSet<>();
        for (String s : this.riskInfo) {
            String keyWord = s.substring(0, s.indexOf(":"));
            if (keyWord.toLowerCase().equals("method") && !s.contains("void"))
                res.add(s);
            if (keyWord.toLowerCase().equals("field"))
                res.add(s);
            if (keyWord.toLowerCase().equals("keystring"))
                res.add(s);
        }
        return res;
    }
}
