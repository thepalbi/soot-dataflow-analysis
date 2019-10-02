package analysis;

import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.HIGH;
import static analysis.abstraction.SensibilityLattice.supremeBetween;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import analysis.abstraction.SensibilityLattice;
import org.slf4j.Logger;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

// TODO: Maybe it would be nice for the analysis to keep in the dataflow a trace from where each sensible data was originated.

// TODO: Make this inter-procedural.

// TODO: Add sensibility levels.

// TODO: Add sanitization, and a larger collection of offending methods, even ones as VirtualInvokes, and possibly load all of them from a config file.

public class SensibleDataAnalysis extends ForwardFlowAnalysis<Unit, Map<String, SensibilityLattice>> {

  private final Logger LOGGER = getLogger(SensibleDataAnalysis.class);

  private Map<String, SensibilityLattice> startingLocalsMap;
  private Map<Unit, Boolean> possibleLeakInUnit;
  private final Map<Integer, SensibilityLattice> methodParams;
  private boolean returningSensibleValue = false;

  public static SensibleDataAnalysis forBody(Body body) {
    return new SensibleDataAnalysis(new ExceptionalUnitGraph(body), new HashMap<>());
  }

  /**
   * Creates a new {@link SensibleDataAnalysis} for the given body, and method params
   * 
   * @param body
   * @param params the method params sensibility map
   * @return
   */
  public static SensibleDataAnalysis forBodyAndParams(Body body, Map<Integer, SensibilityLattice> params) {
    return new SensibleDataAnalysis(new ExceptionalUnitGraph(body), params);
  }

  public SensibleDataAnalysis(ExceptionalUnitGraph graph, Map<Integer, SensibilityLattice> methodParams) {
    super(graph);

    startingLocalsMap = new HashMap<>();
    possibleLeakInUnit = new HashMap<>();
    this.methodParams = methodParams;

    // As starting point, save all locals as bottom
    for (Local variable : graph.getBody().getLocals()) {
      this.startingLocalsMap.put(variable.getName(), SensibilityLattice.getBottom());
    }

    doAnalysis();
  }

  @Override
  protected void flowThrough(Map<String, SensibilityLattice> in, Unit unit,
                             Map<String, SensibilityLattice> out) {

    StatementVisitor visitor = new StatementVisitor(in, methodParams).visit((Stmt) unit);

    possibleLeakInUnit.put(unit, visitor.getDoesStatementLeak());
    // Since a return statement is last in the CFG, it's not needed to prevent overwrites
    returningSensibleValue = visitor.getReturningSensibleValue();

    out.clear();
    out.putAll(in);
  }

  private void markNewSensibleLocal(Map<String, SensibilityLattice> in, String localName) {
    LOGGER.debug("Just discovered a sensible variable named {}", localName);
    in.put(localName, HIGH);
  }

  public boolean possibleLeakInUnit(Unit unit) {
    return possibleLeakInUnit.getOrDefault(unit, false);
  }

  public List<Unit> getOffendingUnits() {
    return possibleLeakInUnit.keySet().stream().collect(Collectors.toList());
  }

  @Override
  protected Map<String, SensibilityLattice> newInitialFlow() {
    Map<String, SensibilityLattice> newMap = new HashMap<>();
    newMap.putAll(startingLocalsMap);
    return newMap;
  }

  @Override
  protected void merge(Map<String, SensibilityLattice> input1, Map<String, SensibilityLattice> input2,
                       Map<String, SensibilityLattice> out) {
    out.clear();
    out.putAll(input1);
    // On conflicting values, take supreme to make analysis sound
    for (String variable : input2.keySet()) {
      SensibilityLattice currentValue = out.getOrDefault(variable, BOTTOM);
      out.put(variable, supremeBetween(input2.get(variable), currentValue));
    }

  }

  @Override
  protected void copy(Map<String, SensibilityLattice> input, Map<String, SensibilityLattice> out) {
    out.clear();
    out.putAll(input);
  }

  public boolean isReturningSensibleValue() {
    return returningSensibleValue;
  }
}
