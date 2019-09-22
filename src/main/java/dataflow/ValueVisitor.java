package dataflow;

import soot.Local;
import soot.Value;

public interface ValueVisitor<T> {

  ValueVisitor visit(Value value);

  void visitLocal(Local variable);

  default void visitDivExpression(T leftOperand, T rightOperand) {}

  default void visitMulExpression(T leftOperand, T rightOperand) {}

  default void visitSubExpression(T leftOperand, T rightOperand) {}

  default void visitAddExpression(T leftOperand, T rightOperand) {}

  default void visitIntegerConstant(int value) {}

  T done();

  ValueVisitor cloneVisitor();

}
