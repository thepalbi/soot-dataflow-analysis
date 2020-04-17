package analysis.abstraction;

import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.util.TriConsumer;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

/**
 * A {@link BiConsumer} which represent a function that takes a {@link SootMethod} and an arguments {@link List<Value>},
 * and causes a side effect if it applies.
 */
public interface InvokeFunction extends TriConsumer<InvokeStmt, SootMethod, List<Value>> {

    /**
     * Decides whether or not this function is applicable to the supplied {@link SootMethod}.
     *
     * @param invocation
     * @param method     the method on which to apply the function
     * @return
     */
    boolean applies(InvokeStmt invocation, SootMethod method);
}
