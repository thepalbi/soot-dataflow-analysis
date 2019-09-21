package dataflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dataflow.abs.ValueToZeroLatticeMap;
import soot.Unit;
import soot.Value;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Division by zero analysis
 */
public class ZeroAnalysis extends ForwardFlowAnalysis<Unit, ValueToZeroLatticeMap> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZeroAnalysis.class);
  private IsZeroVisitor visitor;

  public ZeroAnalysis(DirectedGraph<Unit> graph) {
    super(graph);

    visitor = new IsZeroVisitor();

    doAnalysis();
  }

  public Boolean isVariableIntegerInvolved(Value aValue) {
    return visitor.varaibleIsInteger.get(aValue);
  }

  protected void flowThrough(ValueToZeroLatticeMap flowSet, Unit unit, ValueToZeroLatticeMap a1) {

    visitor.visit(unit);

    // It's an assignment / definition
    /*
     * if (unit instanceof DefinitionStmt) { DefinitionStmt newDefinition = (DefinitionStmt) unit; Value value =
     * newDefinition.getRightOp(); if (value.getType() instanceof IntegerType) { // Check is assignment is some binary expression
     * if (value instanceof BinopExpr) { BinopExpr expr = (BinopExpr) value; unit.addTag(new
     * StringTag("This is an operation with operator: " + expr.getSymbol())); } else { // It must be a simple value
     * unit.addTag(new StringTag("This is an integer assignment")); } }
     * 
     * }
     */
  }

  protected ValueToZeroLatticeMap newInitialFlow() {
    return new ValueToZeroLatticeMap();
  }

  protected void merge(ValueToZeroLatticeMap input1, ValueToZeroLatticeMap input2, ValueToZeroLatticeMap output) {
    output.putAll(input1);
    output.putAll(input2);
  }

  protected void copy(ValueToZeroLatticeMap source, ValueToZeroLatticeMap dest) {
    dest.putAll(source);
  }
}
