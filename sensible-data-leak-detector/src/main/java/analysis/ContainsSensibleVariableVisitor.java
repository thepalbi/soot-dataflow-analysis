package analysis;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import soot.Local;
import soot.SootClass;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import wtf.thepalbi.PointsToResult;

import java.util.HashMap;
import java.util.Map;

import static analysis.StatementVisitor.getArgumentSensibilityFor;
import static analysis.StatementVisitor.someValueApplies;
import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.isSensible;

/**
 * {@link dataflow.utils.ValueVisitor} that checks whether a {@link soot.Value} is sensible accordin to the method
 * arguments, or the locals sensibility level.
 */
public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Boolean> {

    private final Map<String, SensibilityLattice> localSensibilityLevel;
    private Map<Integer, SensibilityLattice> parametersSensibility;
    private Boolean isSensible;
    private SootClass mainClass;
    private PointsToResult pointsTo;

    public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel, SootClass mainClass, PointsToResult pointsTo) {
        this(localsSensibilityLevel, new HashMap<>(), mainClass, pointsTo);
    }

    public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel,
                                           Map<Integer, SensibilityLattice> parametersSensibility,
                                           SootClass mainClass,
                                           PointsToResult pointsTo) {
        this.localSensibilityLevel = localsSensibilityLevel;
        this.parametersSensibility = parametersSensibility;
        this.mainClass = mainClass;
        this.pointsTo = pointsTo;
        this.isSensible = false;
    }

    @Override
    public Boolean done() {
        return isSensible;
    }

    @Override
    public ContainsSensibleVariableVisitor cloneVisitor() {
        return new ContainsSensibleVariableVisitor(localSensibilityLevel, parametersSensibility, mainClass, pointsTo);
    }

    @Override
    protected void visitLocal(Local variable) {
        this.isSensible = isSensible(localSensibilityLevel.get(variable.getName()));
    }

    @Override
    protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {
        if (instanceInvokeExpr instanceof InterfaceInvokeExpr) {
            // Interface invoke. Use points to to resolve.

        } else {
            // The method is not an interface method. Can be resolved from class.

        }
        // TODO: Maybe change to maybe sensible in this cases
        // Maybe instance whose method is being invoked is sensible
        // Split here into which kind of instance invocation this is
        isSensible = this.cloneVisitor().visit(instanceInvokeExpr.getBase()).done();
        if (!isSensible) {
            // Or maybe some of its arguments are sensible
            visitInvokeExpr(instanceInvokeExpr);
        }
    }

    @Override
    protected void visitInvokeExpr(InvokeExpr invokeExpr) {
        // Redo this to handle any method as long as Soot has its body. If not log and decide what to do.
        if (invokeExpr.getMethod().getDeclaringClass().equals(mainClass)) {
            // Method defined in same package as main class
            isSensible = SensibleDataAnalysis.forBodyAndParams(invokeExpr.getMethod().getActiveBody(),
                    getArgumentSensibilityFor(localSensibilityLevel, invokeExpr.getArgs()), pointsTo)
                    .isReturningSensibleValue();
        } else {
            // This is shit
            isSensible = someValueApplies(invokeExpr.getArgs(), this.cloneVisitor());
        }
    }

    @Override
    protected void visitParameterRef(ParameterRef parameter) {
        SensibilityLattice valueForParameter = parametersSensibility.getOrDefault(parameter.getIndex(), BOTTOM);
        this.isSensible = isSensible(valueForParameter);
    }

}
