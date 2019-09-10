package dataflow;

import dataflow.abs.ZeroLattice;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class ZeroAnalysis extends ForwardFlowAnalysis<Unit, ZeroLattice> {

  public ZeroAnalysis(DirectedGraph<Unit> graph) {
    super(graph);
    // Internal structures initialization here
    doAnalysis();
  }

  protected void flowThrough(ZeroLattice zeroLattice, Unit unit, ZeroLattice a1) {

  }

  protected ZeroLattice newInitialFlow() {
    return null;
  }

  protected void merge(ZeroLattice zeroLattice, ZeroLattice a1, ZeroLattice a2) {

  }

  protected void copy(ZeroLattice zeroLattice, ZeroLattice a1) {

  }
}
