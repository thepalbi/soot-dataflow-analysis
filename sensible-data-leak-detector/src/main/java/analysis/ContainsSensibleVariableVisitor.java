package analysis;

import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.isSensible;
import static java.util.Optional.empty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import soot.IntType;
import soot.Local;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.internal.JimpleLocal;

public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Optional<Local>> {

  private final Map<String, SensibilityLattice> localSensibilityLevel;
  private Map<Integer, SensibilityLattice> parametersSensibility;
  private Optional<Local> sensibleVariable = empty();

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel) {
    this(localsSensibilityLevel, new HashMap<>());
  }

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel,
                                         Map<Integer, SensibilityLattice> parametersSensibility) {
    this.localSensibilityLevel = localsSensibilityLevel;
    this.parametersSensibility = parametersSensibility;
  }

  @Override
  public Optional<Local> done() {
    return sensibleVariable;
  }

  @Override
  public ContainsSensibleVariableVisitor cloneVisitor() {
    return new ContainsSensibleVariableVisitor(localSensibilityLevel, parametersSensibility);
  }

  @Override
  protected void visitLocal(Local variable) {
    if (isSensible(localSensibilityLevel.get(variable.getName()))) {
      sensibleVariable = Optional.of(variable);
    }
  }

  @Override
  protected void visitInvokeExpr(InvokeExpr invokeExpr) {
    // TODO: Maybe the method called here is offending. Add check
    sensibleVariable = invokeExpr.getArgs()
        .stream()
        .filter(argument -> this.cloneVisitor().visit(argument).done().isPresent())
        .findFirst()
        .map(value -> (Local) value);
  }

  @Override
  protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {
    // TODO: Maybe change to maybe sensible in this cases
    // Maybe instance whose method is being invoked is sensible
    sensibleVariable = this.cloneVisitor().visit(instanceInvokeExpr.getBase()).done();
    if (!sensibleVariable.isPresent()) {
      // Or maybe some of its arguments are sensible
      visitInvokeExpr(instanceInvokeExpr);
    }
  }

  @Override
  protected void visitParameterRef(ParameterRef parameter) {
    SensibilityLattice valueForParameter = parametersSensibility.getOrDefault(parameter.getIndex(), BOTTOM);
    if (isSensible(valueForParameter)) {
      // TODO: API is kind of shity, should not use as result the offending local I'm not using it
      sensibleVariable = Optional.of(new JimpleLocal("fake", new IntType(null)));
    }
  }
}
