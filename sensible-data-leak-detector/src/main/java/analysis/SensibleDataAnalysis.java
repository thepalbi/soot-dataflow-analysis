package analysis;

import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.HIGH;
import static analysis.abstraction.SensibilityLattice.supremeBetween;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import analysis.abstraction.SensibilityLattice;
import javafx.util.Pair;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
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
  private Set<String> offendingMethod;
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

    offendingMethod = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                                                                              "println", "print")));

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
    if (unit instanceof DefinitionStmt) {
      DefinitionStmt definition = (DefinitionStmt) unit;
      new ContainsSensibleVariableVisitor(in, methodParams).visit(definition.getRightOp()).done()
          .ifPresent(sensibleLocalUsed -> markNewSensibleLocal(in, (Local) definition.getLeftOp()));
    } else if (unit instanceof InvokeStmt) {
      InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
      SootMethod invokedMethod = invokeExpr.getMethod();
      if (invokedMethod.getDeclaringClass().getName().equals("analysis.SensibilityMarker") &&
          invokedMethod.getName().equals("markAsSensible")) {
        // Marking the argument as sensible, if it's a local
        assert invokeExpr.getArgCount() == 1;
        Value argument = invokeExpr.getArg(0);
        if (argument instanceof Local) {
          markNewSensibleLocal(in, (Local) argument);
        }
      } else if (offendingMethod.contains(invokedMethod.getName())) {
        // This method is offending, if it has a sensible variable, WARN
        LOGGER.debug("Just found offending method call");
        invokeExpr.getArgs().stream()
            .filter(argument -> new ContainsSensibleVariableVisitor(in).visit(argument).done().isPresent())
            .findFirst()
            .ifPresent(sensibleArgument -> {
              LOGGER.debug("Local variable named {} is being leaked", sensibleArgument);
              possibleLeakInUnit.put(unit, true);
            });
      } else if (invokedMethod.getDeclaringClass().getPackageName().equals("soot")) {
        // Maybe this method leaks some sensible variable. Run analysis on method
        // Collect params sensibility
        Map<Integer, SensibilityLattice> paramsSensibility = getArgumentSensibilityFor(in, invokeExpr);
        SensibleDataAnalysis calledMethodAnalysis =
            SensibleDataAnalysis.forBodyAndParams(invokedMethod.getActiveBody(), paramsSensibility);
        if (!calledMethodAnalysis.getOffendingUnits().isEmpty()) {
          possibleLeakInUnit.put(unit, true);
        }
      }
    } else if (unit instanceof ReturnStmt) {
      ReturnStmt ret = (ReturnStmt) unit;
      returningSensibleValue = new ContainsSensibleVariableVisitor(in).visit(ret.getOp()).done().isPresent();
    }
    out.clear();
    out.putAll(in);
  }

  public static Map<Integer, SensibilityLattice> getArgumentSensibilityFor(Map<String, SensibilityLattice> in,
                                                                           InvokeExpr invokeExpr) {
    AtomicInteger index = new AtomicInteger(0);
    return invokeExpr.getArgs().stream()
        .map(value -> new Pair<>(index.getAndIncrement(), value))
        .filter(paramPair -> paramPair.getValue() instanceof Local)
        .map(paramPair -> new Pair<>(paramPair.getKey(),
                                     in.getOrDefault(((Local) paramPair.getValue()).getName(),
                                                     BOTTOM)))
        .collect(Collectors.toMap(pair -> pair.getKey(), pair -> pair.getValue()));
  }

  private void markNewSensibleLocal(Map<String, SensibilityLattice> in, Local argument) {
    Local sensibleLocal = argument;
    LOGGER.debug("Just discovered a sensible variable named {}", sensibleLocal.getName());
    in.put(sensibleLocal.getName(), HIGH);
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
