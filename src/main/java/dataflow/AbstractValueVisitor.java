package dataflow;

import dataflow.errors.VisitorNotImplementedForType;
import soot.Local;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.DivExpr;
import soot.jimple.IntConstant;
import soot.jimple.MulExpr;
import soot.jimple.SubExpr;

public abstract class AbstractValueVisitor<T> implements ValueVisitor<T> {

  @Override
  public ValueVisitor<T> visit(Value value) {
    if (value instanceof IntConstant) {
      visitIntegerConstant(((IntConstant) value).value);
    } else if (value instanceof BinopExpr) {
      doVisitBinaryExpression((BinopExpr) value);
    } else if (value instanceof Local) {
      visitLocal((Local) value);
    }
    return this;
  }

  public void doVisitBinaryExpression(BinopExpr value) {
    BinopExpr expr = value;
    // TODO: Figure out why the cast is needed here
    T leftVisitorResult = (T) cloneVisitor().visit(expr.getOp1()).done();
    T rightVisitorResult = (T) cloneVisitor().visit(expr.getOp2()).done();

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
}
