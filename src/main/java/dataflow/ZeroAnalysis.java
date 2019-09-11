package dataflow;

import soot.IntType;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Division by zero analysis
 */
public class ZeroAnalysis extends ForwardFlowAnalysis<Stmt, FlowSet> {

  public ZeroAnalysis(DirectedGraph<Stmt> graph) {
    super(graph);
    doAnalysis();
  }

  protected void flowThrough(FlowSet flowSet, Stmt unit, FlowSet a1) {

    // It's an assignment / definition
    if (unit instanceof DefinitionStmt) {
      DefinitionStmt newDefinition = (DefinitionStmt) unit;
      Value variableName = newDefinition.getLeftOp();
      Value value = newDefinition.getRightOp();

      // The assigned value is int, check if an the right value is an operation or a simple assignment
      if (value.getType() instanceof IntType) {
        if (value instanceof BinopExpr) {
          BinopExpr operation = (BinopExpr) value;
          if (operation.getSymbol().equalsIgnoreCase(" / ")) {
            // Handle division by ZERO

          } else {

          }
        }

      }
    }
  }

  protected FlowSet newInitialFlow() {
    return null;
  }

  protected void merge(FlowSet flowSet, FlowSet a1, FlowSet a2) {

  }

  protected void copy(FlowSet flowSet, FlowSet a1) {

  }
}
