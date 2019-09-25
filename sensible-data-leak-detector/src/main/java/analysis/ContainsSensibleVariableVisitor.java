package analysis;

import static analysis.abstraction.SensibilityLattice.isSensible;
import static java.util.Optional.empty;

import java.util.Map;
import java.util.Optional;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import soot.Local;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;

public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Optional<Local>> {

  private final Map<String, SensibilityLattice> localSensibilityLevel;
  private Optional<Local> sensibleVaraible = empty();

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel) {
    this.localSensibilityLevel = localsSensibilityLevel;
  }

  @Override
  public Optional<Local> done() {
    return sensibleVaraible;
  }

  @Override
  public ContainsSensibleVariableVisitor cloneVisitor() {
    return new ContainsSensibleVariableVisitor(localSensibilityLevel);
  }

  @Override
  protected void visitLocal(Local variable) {
    if (isSensible(localSensibilityLevel.get(variable.getName()))) {
      sensibleVaraible = Optional.of(variable);
    }
  }

  @Override
  protected void visitInvokeExpr(InvokeExpr invokeExpr) {
    // TODO: Maybe the method called here is offending. Add check
    sensibleVaraible = invokeExpr.getArgs()
        .stream()
        .filter(argument -> this.cloneVisitor().visit(argument).done().isPresent())
        .findFirst()
        .map(value -> (Local) value);
  }

  @Override
  protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {
    // TODO: Maybe change to maybe sensible in this cases
    // Maybe instance whose method is being invoked is sensible
    sensibleVaraible = this.cloneVisitor().visit(instanceInvokeExpr.getBase()).done();
    if (!sensibleVaraible.isPresent()) {
      // Or maybe some of its arguments are sensible
      visitInvokeExpr(instanceInvokeExpr);
    }
  }
}
