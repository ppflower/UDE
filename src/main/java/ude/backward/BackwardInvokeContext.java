package ude.backward;

import soot.RefType;
import soot.Value;

import java.util.HashMap;

public class BackwardInvokeContext {

    private boolean isInvokeObjectTainted;
    private RefType invokeObjectType;

    private boolean isResultTainted;
    private HashMap<Integer, Value> paramIndex2Arg;

    public BackwardInvokeContext(boolean isInvokeObjectTainted,
                                 RefType invokeObjectType,
                                 boolean isResultTainted,
                                 HashMap<Integer, Value> paramIndex2Arg) {
        this.isInvokeObjectTainted = isInvokeObjectTainted;
        this.invokeObjectType = invokeObjectType;
        this.isResultTainted = isResultTainted;
        this.paramIndex2Arg = paramIndex2Arg;
    }

    public boolean isThisObjectTainted() {
        return isInvokeObjectTainted;
    }

    public RefType getThisObjectType() {
        return invokeObjectType;
    }

    public boolean isResultTainted() {
        return isResultTainted;
    }

    public HashMap<Integer, Value> getParamIndex2Arg() {
        return paramIndex2Arg;
    }
}
