package analysis;

import java.util.Map;

import analysis.abstraction.SensibilityLattice;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class SensibleDataAnalysis extends ForwardFlowAnalysis<Unit, Map<String, SensibilityLattice>> {

  public SensibleDataAnalysis(ExceptionalUnitGraph graph) {
    super(graph);

    doAnalysis();
  }

  @Override
  protected void flowThrough(Map<String, SensibilityLattice> in, Unit unit,
                             Map<String, SensibilityLattice> out) {

  }

  @Override
  protected Map<String, SensibilityLattice> newInitialFlow() {
    return null;
  }

  @Override
  protected void merge(Map<String, SensibilityLattice> input1, Map<String, SensibilityLattice> input2,
                       Map<String, SensibilityLattice> out) {

  }

  @Override
  protected void copy(Map<String, SensibilityLattice> input, Map<String, SensibilityLattice> out) {
    out.clear();
    out.putAll(input);
  }
}
