package analysis;

import analysis.abstraction.SensibilityLattice;
import org.slf4j.Logger;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
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
                LOGGER.warn("Cannot resolve points to set in call: {}", instanceInvokeExpr.toString());
                return InvocationResult.noResult();
            }

            List<SootMethod> resolvedMethods = basePointsTo.stream()
                    .map(heapObject -> Scene.v().getSootClass(heapObject.getType()).getMethod(instanceInvokeExpr.getMethodRef().getSubSignature()))
                    .collect(toList());

            boolean someMethodReturnsSensibleValue = false;
            boolean someMethodCallLeaks = false;

            // Go through every resolved method from the points-to set from the invocation base
            // Run the Sensibility analysis and merge back the result into this visitor
            for (SootMethod resolvedMethod : resolvedMethods) {
                InvocationResult result;
                if (!resolvedMethod.hasActiveBody()) {
                    LOGGER.warn("Ignoring interface call to {}, on invocation {}. NO ACTIVE BODY",
                            resolvedMethod.getSignature(),
                            instanceInvokeExpr.toString());
                    result = handleNoMethodBodyCall();
                } else {
                    result = analyzeCalledMethod(resolvedMethod, instanceInvokeExpr.getArgs());
                }

                // If any of the resolved methods calls gives back a sensible value, mark this to be may-sound.
                someMethodReturnsSensibleValue |= result.returnsSensibleValue;
                someMethodCallLeaks |= result.leakInCall;
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
        return new InvocationResult(analysisResult.leaksSensibleValue(), analysisResult.isReturningSensibleValue());
    }

    private InvocationResult handleResolvedInvocation() {
        if (!invokeExpr.getMethod().hasActiveBody()) {
            LOGGER.warn("Ignoring non-interface call to {}, on invocation {}. NO ACTIVE BODY",
                    invokeExpr.getMethod().getSignature(),
                    invokeExpr.toString());
            return handleNoMethodBodyCall();
        }
        return analyzeCalledMethod(invokeExpr.getMethod(), invokeExpr.getArgs());
    }

    private InvocationResult handleNoMethodBodyCall() {
        // By default, assume that most non method body calls with one sensible parameter
        // will correspond to something like String.concat / StringBuilder.append.
        // Assume result sensible

        boolean hasSensibleParameter = invokeExpr.getArgs().stream()
                .filter(value -> value instanceof Local)
                .map(parameter -> ctx.localsSensibility.get(((Local) parameter).getName()))
                .map(SensibilityLattice::isSensible)
                .anyMatch(v -> v);

        boolean receiverIsSensible = false;
        // Also, if the receiver is a sensible value, consider the returned value will also be
        if (invokeExpr instanceof InstanceInvokeExpr && ((InstanceInvokeExpr) invokeExpr).getBase() instanceof Local) {
            Local base = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
            receiverIsSensible = SensibilityLattice.isSensible(ctx.localsSensibility.get(base.getName()));
        }

        // Check that method is non-void returning
        boolean methodIsVoid = invokeExpr.getMethodRef().getReturnType() instanceof VoidType;

        // Check both conditions above
        return new InvocationResult(
                false,
                !methodIsVoid && (hasSensibleParameter || receiverIsSensible));
    }

    public static class InvocationResult {
        public final boolean leakInCall;
        public final boolean returnsSensibleValue;

        public static InvocationResult noResult() {
            return new InvocationResult(false, false);
        }

        public InvocationResult(boolean leakInCall, boolean returnsSensibleValue) {
            this.leakInCall = leakInCall;
            this.returnsSensibleValue = returnsSensibleValue;
        }
    }
}
