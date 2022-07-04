package ude.forward;

import javafx.util.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import java.util.ArrayList;
import java.util.List;

public class TaintPath {
    public FlowSet<Unit> riskUnits;
    public List<Pair<SootMethod, String>> riskInvokes;

    public List<String> taintStrings;

    public TaintPath() {
        riskUnits = new ArraySparseSet<>();
        riskInvokes = new ArrayList<>();

        taintStrings = new ArrayList<>();
    }
}
