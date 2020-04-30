package analysis;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import org.slf4j.Logger;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import wtf.thepalbi.HeapObject;
import wtf.thepalbi.PointsToResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static analysis.StatementVisitor.getArgumentSensibilityFor;
import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.isSensible;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link dataflow.utils.ValueVisitor} that checks whether a {@link soot.Value} is sensible accordin to the method
 * arguments, or the locals sensibility level.
 */
public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Boolean> {

    private final Logger LOGGER = getLogger(ContainsSensibleVariableVisitor.class);
    private final Map<String, SensibilityLattice> localSensibilityLevel;
    private final SootMethod inMethod;
    private Map<Integer, SensibilityLattice> parametersSensibility;
    private Boolean isSensible;
    private SootClass mainClass;
    private PointsToResult pointsTo;

    public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel, SootMethod inMethod, PointsToResult pointsTo) {
        this(localsSensibilityLevel, new HashMap<>(), inMethod, pointsTo);
    }

    public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel,
                                           Map<Integer, SensibilityLattice> parametersSensibility,
                                           SootMethod inMethod,
                                           PointsToResult pointsTo) {
        this.localSensibilityLevel = localsSensibilityLevel;
        this.parametersSensibility = parametersSensibility;
        this.inMethod = inMethod;
        this.pointsTo = pointsTo;
        this.isSensible = false;
    }

    @Override
    public Boolean done() {
        return isSensible;
    }

    @Override
    public ContainsSensibleVariableVisitor cloneVisitor() {
        return new ContainsSensibleVariableVisitor(localSensibilityLevel, parametersSensibility, inMethod, pointsTo);
    }

    @Override
    protected void visitLocal(Local variable) {
        this.isSensible = isSensible(localSensibilityLevel.get(variable.getName()));
    }

    @Override
    protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {
        if (instanceInvokeExpr instanceof InterfaceInvokeExpr) {
            // Interface invoke. Use points to to resolve.
            // Assuming that the base will be a local
            Local base = (Local) instanceInvokeExpr.getBase();
            List<HeapObject> basePointsTo = pointsTo.localPointsTo(inMethod, base.getName());
            // Failed if nothing is resolved in points to set
            if (basePointsTo.isEmpty()) {
                throw new RuntimeException("Cannot resolve points to set in call: " + instanceInvokeExpr.toString());
            }
            List<SootMethod> resolvedMethods = basePointsTo.stream()
                    .map(heapObject -> Scene.v().getSootClass(heapObject.getType()).getMethod(instanceInvokeExpr.getMethodRef().getSubSignature()))
                    .collect(toList());

            // Go through every resolved method from the points-to set from the invocation base
            // Run the Sensibility analysis and merge back the result into this visitor
            for (SootMethod resolvedMethod : resolvedMethods) {
                if (!resolvedMethod.hasActiveBody()) {
                    LOGGER.warn("Ignoring interface call to {}, on invocation {}. NO ACTIVE BODY",
                            resolvedMethod.getSignature(),
                            instanceInvokeExpr.toString());
                    continue;
                }

                // TODO: Check for side effects (if the called method leaks a sensible value)
                // If any of the resolved methods calls gives back a sensible value, mark this to be may-sound.
                isSensible |= analyzeCalledMethod(resolvedMethod, instanceInvokeExpr.getArgs());
            }
        } else {
            handleResolvedInvocation(instanceInvokeExpr);
        }
    }

    private void handleResolvedInvocation(InvokeExpr invokeExpr) {
        if (!invokeExpr.getMethod().hasActiveBody()) {
            LOGGER.warn("Ignoring interface call to {}, on invocation {}. NO ACTIVE BODY",
                    invokeExpr.getMethod().getSignature(),
                    invokeExpr.toString());
            return;
        }
        // TODO: Check for side effects (if the called method leaks a sensible value)
        isSensible = analyzeCalledMethod(invokeExpr.getMethod(), invokeExpr.getArgs());
    }

    @Override
    protected void visitStaticInvokeExpr(InvokeExpr staticInvokeExpr) {
        handleResolvedInvocation(staticInvokeExpr);
    }

    private boolean analyzeCalledMethod(SootMethod calledMethod, List<Value> arguments) {
        return SensibleDataAnalysis.forBodyAndParams(calledMethod.getActiveBody(),
                getArgumentSensibilityFor(localSensibilityLevel, arguments), pointsTo)
                .isReturningSensibleValue();
    }

    @Override
    protected void visitParameterRef(ParameterRef parameter) {
        SensibilityLattice valueForParameter = parametersSensibility.getOrDefault(parameter.getIndex(), BOTTOM);
        this.isSensible = isSensible(valueForParameter);
    }

}
