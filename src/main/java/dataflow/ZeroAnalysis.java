package dataflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Division by zero analysis
 */
public class ZeroAnalysis extends ForwardFlowAnalysis<Unit, VariableToLatticeMap> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZeroAnalysis.class);

  public ZeroAnalysis(DirectedGraph<Unit> graph) {
    super(graph);

    doAnalysis();
  }

  protected void flowThrough(VariableToLatticeMap in, Unit unit, VariableToLatticeMap out) {
    new IsZeroVisitor(in).visit(unit);

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
