package dataflow.abs;

import static dataflow.abs.ZeroLattice.NOT_ZERO;
import static dataflow.abs.ZeroLattice.ZERO;

import dataflow.VariableToLatticeMap;
import dataflow.errors.VisitorNotImplementedForType;
import soot.Local;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.DivExpr;
import soot.jimple.IntConstant;
import soot.jimple.MulExpr;
import soot.jimple.SubExpr;

public class ValueVisitor {

  private final VariableToLatticeMap variables;
  private ZeroLattice resolvedValue = ZeroLattice.BOTTOM;

  public ValueVisitor(VariableToLatticeMap variables) {
    this.variables = variables;
  }

  public ValueVisitor visit(Value value) {
    if (value instanceof IntConstant) {
      visitIntegerConstant(((IntConstant) value).value);
    } else if (value instanceof BinopExpr) {
      doVisitBinaryExpression((BinopExpr) value);
    } else if (value instanceof Local) {
      visitLocal((Local) value);
    }
    return this;
  }

  private void doVisitBinaryExpression(BinopExpr value) {
    BinopExpr expr = value;
    ZeroLattice leftVisitorResult = new ValueVisitor(variables).visit(expr.getOp1()).done();
    ZeroLattice rightVisitorResult = new ValueVisitor(variables).visit(expr.getOp2()).done();

    if (expr instanceof AddExpr) {
      visitAddExpression(leftVisitorResult, rightVisitorResult);
    } else if (expr instanceof SubExpr) {
      visitSubExpression(leftVisitorResult, rightVisitorResult);
    } else if (expr instanceof MulExpr) {
      visitMulExpression(leftVisitorResult, rightVisitorResult);
    } else if (expr instanceof DivExpr) {
      visitDivExpression(leftVisitorResult, rightVisitorResult);
    } else {
      throw new VisitorNotImplementedForType(expr.getClass().getName());
    }
  }

  private void visitLocal(Local variable) {
    resolvedValue = variables.get(variable.getName());
  }

  private void visitDivExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.divideBy(rightOperand);
  }

  private void visitMulExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.multiplyBy(rightOperand);
  }

  private void visitSubExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.substract(rightOperand);
  }

  private void visitAddExpression(ZeroLattice leftOperand, ZeroLattice rightOperand) {
    resolvedValue = leftOperand.add(rightOperand);
  }

  private void visitIntegerConstant(int value) {
    if (value > 0) {
      resolvedValue = NOT_ZERO;
    } else if (value == 0) {
      resolvedValue = ZERO;
    } else {
      throw new RuntimeException("This analysis only supports positive integers");
    }
  }

  public ZeroLattice done() {
    return resolvedValue;
  }
}
