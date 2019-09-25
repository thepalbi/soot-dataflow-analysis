package analysis;

import static analysis.abstraction.SensibilityLattice.isSensible;

import java.util.Map;

import analysis.abstraction.SensibilityLattice;
import dataflow.utils.AbstractValueVisitor;
import dataflow.utils.ValueVisitor;
import soot.Local;

public class ContainsSensibleVariableVisitor extends AbstractValueVisitor<Boolean> {

  private final Map<String, SensibilityLattice> localSensibilityLevel;
  private Boolean isSensible = false;

  public ContainsSensibleVariableVisitor(Map<String, SensibilityLattice> localsSensibilityLevel) {
    this.localSensibilityLevel = localsSensibilityLevel;
  }

  @Override
  public Boolean done() {
    return isSensible;
  }

  @Override
  public ValueVisitor cloneVisitor() {
    return new ContainsSensibleVariableVisitor(localSensibilityLevel);
  }

  @Override
  protected void visitLocal(Local variable) {
    isSensible |= isSensible(localSensibilityLevel.get(variable.getName()));
  }
}
