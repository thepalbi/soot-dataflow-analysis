package analysis;

import static analysis.StatementVisitor.getArgumentSensibilityFor;
import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.isSensible;

import java.util.HashMap;
import java.util.Map;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import soot.Local;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;

/**
 * {@link dataflow.utils.ValueVisitor} that checks whether a {@link soot.Value} is sensible accordin to the method arguments, or
 * the locals sensibility level.
 */
public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Boolean> {

  private final Map<String, SensibilityLattice> localSensibilityLevel;
  private Map<Integer, SensibilityLattice> parametersSensibility;
  private Boolean isSensible;

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel) {
    this(localsSensibilityLevel, new HashMap<>());
  }

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel,
                                         Map<Integer, SensibilityLattice> parametersSensibility) {
    this.localSensibilityLevel = localsSensibilityLevel;
    this.parametersSensibility = parametersSensibility;
    this.isSensible = false;
  }

  @Override
  public Boolean done() {
    return isSensible;
  }

  @Override
  public ContainsSensibleVariableVisitor cloneVisitor() {
    return new ContainsSensibleVariableVisitor(localSensibilityLevel, parametersSensibility);
  }

  @Override
  protected void visitLocal(Local variable) {
    this.isSensible = isSensible(localSensibilityLevel.get(variable.getName()));
  }

  @Override
  protected void visitInvokeExpr(InvokeExpr invokeExpr) {
    // TODO: Maybe the method called here is offending. Add check
    if (invokeExpr.getMethod().getDeclaringClass().getPackageName().equals("soot")) {
      // Method defined in same package as main class
      if (SensibleDataAnalysis.forBodyAndParams(invokeExpr.getMethod().getActiveBody(),
                                                getArgumentSensibilityFor(localSensibilityLevel, invokeExpr.getArgs()))
          .isReturningSensibleValue()) {
        isSensible = true;
      }
    } else {
      isSensible = invokeExpr.getArgs().stream()
          .map(value -> this.cloneVisitor().visit(value).done())
          .reduce(false, (foundSensibleSoFar, isCurrentValueSensible) -> foundSensibleSoFar || isCurrentValueSensible);
    }
  }

  @Override
  protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {
    // TODO: Maybe change to maybe sensible in this cases
    // Maybe instance whose method is being invoked is sensible
    isSensible = this.cloneVisitor().visit(instanceInvokeExpr.getBase()).done();
    if (!isSensible) {
      // Or maybe some of its arguments are sensible
      visitInvokeExpr(instanceInvokeExpr);
    }
  }

  @Override
  protected void visitParameterRef(ParameterRef parameter) {
    SensibilityLattice valueForParameter = parametersSensibility.getOrDefault(parameter.getIndex(), BOTTOM);
    this.isSensible = isSensible(valueForParameter);
  }

}
