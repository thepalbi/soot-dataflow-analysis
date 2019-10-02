package dataflow.utils;

import dataflow.errors.VisitorNotImplementedForType;
import soot.Local;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.DivExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.MulExpr;
import soot.jimple.ParameterRef;
import soot.jimple.SubExpr;

/**
 * Abstract visitor implementations. Each new covered {@link Value} sub-type has to be added here.
 * 
 * @param <T>
 */
public abstract class AbstractValueVisitor<T> implements ValueVisitor<T> {

  @Override
  public final ValueVisitor<T> visit(Value value) {
    if (value instanceof IntConstant) {
      visitIntegerConstant(((IntConstant) value).value);
    } else if (value instanceof BinopExpr) {
      doVisitBinaryExpression((BinopExpr) value);
    } else if (value instanceof Local) {
      visitLocal((Local) value);
    } else if (value instanceof InstanceInvokeExpr) {
      visitInstanceInvokeExp((InstanceInvokeExpr) value);
    } else if (value instanceof InvokeExpr) {
      visitInvokeExpr((InvokeExpr) value);
    } else if (value instanceof ParameterRef) {
      visitParameterRef((ParameterRef) value);
    } else if (value instanceof ArrayRef) {
      visitArrayRef((ArrayRef) value);
    }
    return this;
  }

  public final void doVisitBinaryExpression(BinopExpr value) {
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

  protected void visitLocal(Local variable) {}

  protected void visitDivExpression(T leftOperand, T rightOperand) {}

  protected void visitMulExpression(T leftOperand, T rightOperand) {}

  protected void visitSubExpression(T leftOperand, T rightOperand) {}

  protected void visitAddExpression(T leftOperand, T rightOperand) {}

  protected void visitIntegerConstant(int value) {}

  protected void visitInvokeExpr(InvokeExpr invokeExpr) {}

  protected void visitInstanceInvokeExp(InstanceInvokeExpr instanceInvokeExpr) {}

  protected void visitParameterRef(ParameterRef parameter) {}

  protected void visitArrayRef(ArrayRef ref) {}
}
