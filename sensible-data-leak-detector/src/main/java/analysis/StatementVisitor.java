package analysis;


import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.HIGH;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javafx.util.Pair;
import org.slf4j.Logger;

import analysis.abstraction.SensibilityLattice;
import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

public class StatementVisitor {

  private Set<String> offendingMethod = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                                                                                                "println", "print")));

  private final Logger LOGGER = getLogger(StatementVisitor.class);
  private Map<String, SensibilityLattice> localsSensibility;
  private Map<Integer, SensibilityLattice> params;
  private Boolean returningSensibleValue = false;
  private Boolean doesStatementLeak = false;

  public StatementVisitor(Map<String, SensibilityLattice> localsSensibility, Map<Integer, SensibilityLattice> params) {
    this.localsSensibility = localsSensibility;
    this.params = params;
  }

  public StatementVisitor visit(Stmt statement) {
    if (statement instanceof DefinitionStmt) {
      DefinitionStmt definition = (DefinitionStmt) statement;
      visitDefinition(definition.getLeftOp(), definition.getRightOp());
    } else if (statement instanceof InvokeStmt) {
      InvokeStmt invoke = (InvokeStmt) statement;
      visitInvoke(invoke.getInvokeExpr().getMethod(), invoke.getInvokeExpr().getArgs());
    } else if (statement instanceof ReturnStmt) {
      ReturnStmt returnStmt = (ReturnStmt) statement;
      visitReturn(returnStmt.getOp());
    } else {
      LOGGER.debug("Ignoring statement with text: '{}' and class '{}'", statement.toString(), statement.getClass().getName());
    }
    return this;
  }

  private void visitReturn(Value value) {
    returningSensibleValue = new ContainsSensibleVariableVisitor(localsSensibility).visit(value).done();
  }

  private void visitInvoke(SootMethod method, List<Value> arguments) {
    if (isMarkAsSensible(method)) {
      // Marking the argument as sensible, if it's a local
      assert arguments.size() == 1;
      Value argument = arguments.get(0);
      localsSensibility.put(new AssigneeNameExtractor().visit(argument).done(), HIGH);
    } else if (isSanitize(method)) {
      assert arguments.size() == 1;
      Value argument = arguments.get(0);
      localsSensibility.put(new AssigneeNameExtractor().visit(argument).done(), BOTTOM);
    } else if (isOffendingMethod(method)) {
      // This method is offending, if it has a sensible variable, WARN
      LOGGER.debug("Just found offending method call");
      doesStatementLeak = arguments.stream()
          .map(argument -> new ContainsSensibleVariableVisitor(localsSensibility).visit(argument).done())
          .reduce(false, (soFar, currentIsSensible) -> soFar || currentIsSensible);
    } else if (method.getDeclaringClass().getPackageName().equals("soot")) {
      // Maybe this method leaks some sensible variable. Run analysis on method
      // Collect params sensibility
      Map<Integer, SensibilityLattice> paramsSensibility = getArgumentSensibilityFor(localsSensibility, arguments);
      SensibleDataAnalysis calledMethodAnalysis =
          SensibleDataAnalysis.forBodyAndParams(method.getActiveBody(), paramsSensibility);
      if (!calledMethodAnalysis.getOffendingUnits().isEmpty()) {
        doesStatementLeak = true;
      }
    }
  }

  private void visitDefinition(Value assignee, Value value) {
    if (new ContainsSensibleVariableVisitor(localsSensibility, params).visit(value).done()) {
      localsSensibility.put(new AssigneeNameExtractor().visit(assignee).done(), HIGH);
    }
  }

  private boolean isOffendingMethod(SootMethod invokedMethod) {
    return offendingMethod.contains(invokedMethod.getName());
  }

  private boolean isSanitize(SootMethod invokedMethod) {
    return invokedMethodIdentifiedBy(invokedMethod, "analysis.SensibilityMarker", "sanitize");
  }

  private boolean isMarkAsSensible(SootMethod invokedMethod) {
    return invokedMethodIdentifiedBy(invokedMethod, "analysis.SensibilityMarker", "markAsSensible");
  }

  private boolean invokedMethodIdentifiedBy(SootMethod method, String fullClassName, String methodName) {
    return method.getDeclaringClass().getName().equals(fullClassName) &&
        method.getName().equals(methodName);
  }

  public static Map<Integer, SensibilityLattice> getArgumentSensibilityFor(Map<String, SensibilityLattice> locals,
                                                                           List<Value> arguments) {
    AtomicInteger index = new AtomicInteger(0);
    return arguments.stream()
        .map(value -> new Pair<>(index.getAndIncrement(), value))
        .filter(paramPair -> paramPair.getValue() instanceof Local)
        .map(paramPair -> new Pair<>(paramPair.getKey(),
                                     locals.getOrDefault(((Local) paramPair.getValue()).getName(),
                                                         BOTTOM)))
        .collect(Collectors.toMap(pair -> pair.getKey(), pair -> pair.getValue()));
  }

  public Boolean getReturningSensibleValue() {
    return returningSensibleValue;
  }

  public Boolean getDoesStatementLeak() {
    return doesStatementLeak;
  }
}
