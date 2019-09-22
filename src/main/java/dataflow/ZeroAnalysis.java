package dataflow;

import java.util.HashMap;

import dataflow.abs.ZeroLattice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Division by zero analysis
 */
public class ZeroAnalysis extends ForwardFlowAnalysis<Unit, VariableToLatticeMap> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZeroAnalysis.class);

  public HashMap<Unit, ZeroLattice> resolvedForUnit = new HashMap<>();

  public ZeroAnalysis(DirectedGraph<Unit> graph) {
    super(graph);

    doAnalysis();
  }

  protected void flowThrough(VariableToLatticeMap in, Unit unit, VariableToLatticeMap out) {
    StmtVisitor currentVisitor = new StmtVisitor(in);
    currentVisitor.visit((Stmt) unit);

    currentVisitor.resolvedValueIfAssignment.ifPresent(latticeValue -> resolvedForUnit.put(unit, latticeValue));

    out.putAll(in);
  }

  protected VariableToLatticeMap newInitialFlow() {
    return new VariableToLatticeMap();
  }

  protected void merge(VariableToLatticeMap input1, VariableToLatticeMap input2, VariableToLatticeMap output) {
    output.putAll(input1);
    output.putAll(input2);
  }

  protected void copy(VariableToLatticeMap source, VariableToLatticeMap dest) {
    dest.putAll(source);
  }
}
