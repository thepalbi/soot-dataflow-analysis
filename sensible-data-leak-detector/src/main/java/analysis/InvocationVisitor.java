package analysis;

import org.slf4j.Logger;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import wtf.thepalbi.HeapObject;

import java.util.List;

import static analysis.StatementVisitor.getArgumentSensibilityFor;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

public class InvocationVisitor {
    private final SensibleDataAnalysis.Context ctx;
    private final InvokeExpr invokeExpr;
    private final Logger LOGGER = getLogger(InvocationVisitor.class);
    private boolean returnedValueIsSensible;

    public InvocationVisitor(SensibleDataAnalysis.Context ctx, InvokeExpr invokeExpr) {
        this.ctx = ctx;
        this.invokeExpr = invokeExpr;
    }

    public InvocationResult visit() {
        if (invokeExpr instanceof InterfaceInvokeExpr) {
            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
            // Interface invoke. Use points to to resolve.
            // Assuming that the base will be a local
            Local base = (Local) instanceInvokeExpr.getBase();
            List<HeapObject> basePointsTo = ctx.pointsToData.localPointsTo(ctx.inMethod, base.getName());
            // Failed if nothing is resolved in points to set
            if (basePointsTo.isEmpty()) {
                throw new RuntimeException("Cannot resolve points to set in call: " + instanceInvokeExpr.toString());
            }
            List<SootMethod> resolvedMethods = basePointsTo.stream()
                    .map(heapObject -> Scene.v().getSootClass(heapObject.getType()).getMethod(instanceInvokeExpr.getMethodRef().getSubSignature()))
                    .collect(toList());

            boolean someMethodReturnsSensibleValue = false;
            boolean someMethodCallLeaks = false;

            // Go through every resolved method from the points-to set from the invocation base
            // Run the Sensibility analysis and merge back the result into this visitor
            for (SootMethod resolvedMethod : resolvedMethods) {
                if (!resolvedMethod.hasActiveBody()) {
                    LOGGER.warn("Ignoring interface call to {}, on invocation {}. NO ACTIVE BODY",
                            resolvedMethod.getSignature(),
                            instanceInvokeExpr.toString());
                    continue;
                }

                InvocationResult invocationResult = analyzeCalledMethod(resolvedMethod, instanceInvokeExpr.getArgs());
                // If any of the resolved methods calls gives back a sensible value, mark this to be may-sound.
                someMethodReturnsSensibleValue |= invocationResult.returnsSensibleValue;
                someMethodCallLeaks |= invocationResult.leakInCall;
            }
            return new InvocationResult(someMethodCallLeaks, someMethodReturnsSensibleValue);
        } else {
            return handleResolvedInvocation();
        }
    }

    private InvocationResult analyzeCalledMethod(SootMethod calledMethod, List<Value> arguments) {
        SensibleDataAnalysis analysisResult = SensibleDataAnalysis.forBodyAndParams(calledMethod.getActiveBody(),
                getArgumentSensibilityFor(ctx.localsSensibility, arguments), ctx.pointsToData);

        // TODO: Check for side effects (if the called method leaks a sensible value)
        // TODO: Check leak in called method
        return new InvocationResult(false, analysisResult.isReturningSensibleValue());
    }

    private InvocationResult handleResolvedInvocation() {
        if (!invokeExpr.getMethod().hasActiveBody()) {
            LOGGER.warn("Ignoring interface call to {}, on invocation {}. NO ACTIVE BODY",
                    invokeExpr.getMethod().getSignature(),
                    invokeExpr.toString());
            return new InvocationResult(false, false);
        }
        return analyzeCalledMethod(invokeExpr.getMethod(), invokeExpr.getArgs());
    }


    public static class InvocationResult {
        public final boolean leakInCall;
        public final boolean returnsSensibleValue;

        public InvocationResult(boolean leakInCall, boolean returnsSensibleValue) {

            this.leakInCall = leakInCall;
            this.returnsSensibleValue = returnsSensibleValue;
        }
    }
}
