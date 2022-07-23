package ude.forward;

import soot.RefLikeType;
import soot.RefType;
import soot.Value;
import soot.toolkits.scalar.FlowSet;

import java.util.HashMap;

public class ForwardInvokeContext {

    private boolean isInvokeObjectTainted;
    private RefLikeType invokeObjectType;
    private FlowSet<Integer> taintedParamIndexes;
    private HashMap<Integer, Value> paramIndex2Arg;

    private boolean isCallback;

    public ForwardInvokeContext(boolean isInvokeObjectTainted,
                                RefLikeType invokeObjectType,
                                FlowSet<Integer> taintedParamIndexes,
                                HashMap<Integer, Value> paramIndex2Arg) {

        this(isInvokeObjectTainted, invokeObjectType, taintedParamIndexes, paramIndex2Arg, false);
    }

    public ForwardInvokeContext(boolean isInvokeObjectTainted,
                                RefLikeType invokeObjectType,
                                FlowSet<Integer> taintedParamIndexes,
                                HashMap<Integer, Value> paramIndex2Arg,
                                boolean isCallback) {
        this.isInvokeObjectTainted = isInvokeObjectTainted;
        this.invokeObjectType = invokeObjectType;
        this.taintedParamIndexes = taintedParamIndexes;
        this.paramIndex2Arg = paramIndex2Arg;
        this.isCallback = isCallback;
    }

    public boolean isThisObjectTainted() {
        return isInvokeObjectTainted;
    }

    public RefLikeType getThisObjectType() {
        return invokeObjectType;
    }

    public FlowSet<Integer> getTaintedParamIndexes() {
        return taintedParamIndexes;
    }

    public HashMap<Integer, Value> getParamIndex2Arg() {
        return paramIndex2Arg;
    }

    public boolean isCallback() {
        return this.isCallback;
    }

}
