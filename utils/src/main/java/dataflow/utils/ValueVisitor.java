package dataflow.utils;

import soot.Value;

/**
 * A {@link Value} visitor
 * 
 * @param <T> The result type
 */
public interface ValueVisitor<T> {

  /**
   * Visits the given {@link Value}, and recursively visit all used values in order to produce an aggregated result.
   * 
   * @param value the {@link Value} to visit
   * @return
   */
  ValueVisitor<T> visit(Value value);

  /**
   * Gets the result of this visitor. MUST be called after {@link ValueVisitor#visit(Value)}.
   * 
   * @return the result of a visit
   */
  T done();

  /**
   * User-defined clone method, to have a common interface for when a new {@link ValueVisitor} has to be launched against an
   * internal value. For example, in the right operand of a {@link soot.jimple.DefinitionStmt}. Usually used when the visitor
   * needs some collaborators for working.
   * 
   * @return a new visitor
   */
  ValueVisitor<T> cloneVisitor();

}
