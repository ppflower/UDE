package ude.binder;

import soot.SootClass;
import soot.SootMethod;

public interface CallbackLocator {

    SootMethod locateStandardCallback(SootClass sootClass);
}
