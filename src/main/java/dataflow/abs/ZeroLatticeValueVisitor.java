package dataflow.abs;

import static dataflow.abs.ZeroLattice.NOT_ZERO;
import static dataflow.abs.ZeroLattice.ZERO;

import dataflow.AbstractValueVisitor;
import dataflow.ValueVisitor;
import dataflow.VariableToLatticeMap;
import soot.Local;

public class ZeroLatticeValueVisitor extends AbstractValueVisitor<ZeroLattice> {

  private final VariableToLatticeMap variables;
  private ZeroLattice resolvedValue = ZeroLattice.BOTTOM;

  public ZeroLatticeValueVisitor(VariableToLatticeMap variables) {
    this.variables = variables;
  }

  @Override
  public void visitLocal(Local variable) {
    resolvedValue = variables.get(variable.getName());
  }

  @Override
  public void visitDivExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.divideBy(rightOperand);
  }

  @Override
  public void visitMulExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.multiplyBy(rightOperand);
  }

  @Override
  public void visitSubExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.substract(rightOperand);
  }

  @Override
  public void visitAddExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.add(rightOperand);
  }

  @Override
  public void visitIntegerConstant(int value) {
    if (value > 0) {
      resolvedValue = NOT_ZERO;
    } else if (value == 0) {
      resolvedValue = ZERO;
    } else {
      throw new RuntimeException("This analysis only supports positive integers");
    }
  }

  @Override
  public ZeroLattice done() {
    return resolvedValue;
  }

  @Override
  public ValueVisitor cloneVisitor() {
    return new ZeroLatticeValueVisitor(variables);
  }
}
