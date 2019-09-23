package dataflow.utils;

import soot.Value;

public interface ValueVisitor<T> {

  ValueVisitor visit(Value value);

  T done();

  ValueVisitor cloneVisitor();

}
