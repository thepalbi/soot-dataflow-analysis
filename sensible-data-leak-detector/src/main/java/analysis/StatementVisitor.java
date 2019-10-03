package analysis;


import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.HIGH;
import static analysis.abstraction.SensibilityLattice.NOT_SENSIBLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import analysis.abstraction.InvokeFunction;
import analysis.abstraction.SensibilityLattice;
import dataflow.utils.ValueVisitor;
import javafx.util.Pair;
import org.slf4j.Logger;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

/**
 * Visitor for extracting from {@link Stmt} whether or not a sensible value is leaked.
 */
public class StatementVisitor {

  // TODO: Change to immutable list
  private final List<InvokeFunction> invokeFunctions = Arrays.asList(
                                                                     new MarkAsSensibleInvokeFun(),
                                                                     new SanitizeInvokeFunc(),
                                                                     new OffenderInvokeFun(),
                                                                     new LocalInvokeFunc());

  private final Logger LOGGER = getLogger(StatementVisitor.class);
  private Map<String, SensibilityLattice> localsSensibility;
  private Map<Integer, SensibilityLattice> params;
  private SootClass mainClass;
  private Boolean returningSensibleValue = false;
  private Boolean doesStatementLeak = false;

  public StatementVisitor(Map<String, SensibilityLattice> localsSensibility, Map<Integer, SensibilityLattice> params,
                          SootClass mainClass) {
    this.localsSensibility = localsSensibility;
    this.params = params;
    this.mainClass = mainClass;
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
    returningSensibleValue = new ContainsSensibleVariableVisitor(localsSensibility, mainClass).visit(value).done();
  }

  private void visitInvoke(SootMethod method, List<Value> arguments) {
    invokeFunctions.stream()
        .filter(function -> function.applies(method))
        .findFirst()
        .ifPresent(function -> function.accept(method, arguments));
  }

  private void visitDefinition(Value assignee, Value value) {
    SensibilityLattice resolvedLevel =
        new ContainsSensibleVariableVisitor(localsSensibility, params, mainClass).visit(value).done() ? HIGH : NOT_SENSIBLE;
    localsSensibility.put(new AssigneeNameExtractor().visit(assignee).done(), resolvedLevel);
  }

  public static boolean someValueApplies(List<Value> values, ValueVisitor<Boolean> booleanValueVisitor) {
    return values.stream()
        .map(argument -> booleanValueVisitor.visit(argument).done())
        .reduce(false, (soFar, currentIsSensible) -> soFar || currentIsSensible);

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

  private class MarkAsSensibleInvokeFun implements InvokeFunction {

    @Override
    public boolean applies(SootMethod method) {
      return invokedMethodIdentifiedBy(method, "analysis.SensibilityMarker", "markAsSensible");
    }

    @Override
    public void accept(SootMethod method, List<Value> arguments) {
      assert arguments.size() == 1;
      localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), HIGH);
    }
  }

  private class SanitizeInvokeFunc implements InvokeFunction {

    @Override
    public boolean applies(SootMethod method) {
      return invokedMethodIdentifiedBy(method, "analysis.SensibilityMarker", "sanitize");
    }

    @Override
    public void accept(SootMethod method, List<Value> arguments) {
      assert arguments.size() == 1;
      localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), NOT_SENSIBLE);
    }
  }

  private class LocalInvokeFunc implements InvokeFunction {

    @Override
    public boolean applies(SootMethod method) {
      return method.getDeclaringClass().equals(mainClass);
    }

    @Override
    public void accept(SootMethod method, List<Value> arguments) {
      // Maybe this method leaks some sensible variable. Run analysis on method
      // Collect params sensibility
      Map<Integer, SensibilityLattice> paramsSensibility = getArgumentSensibilityFor(localsSensibility, arguments);
      SensibleDataAnalysis calledMethodAnalysis =
          SensibleDataAnalysis.forBodyAndParams(method.getActiveBody(), paramsSensibility);
      if (!calledMethodAnalysis.noLeaksDetected()) {
        doesStatementLeak = true;
      }

    }
  }

  private class OffenderInvokeFun implements InvokeFunction {

    public OffenderInvokeFun() {}

    @Override
    public boolean applies(SootMethod method) {
      return new OffendingMethodPredicate().test(method);
    }

    @Override
    public void accept(SootMethod method, List<Value> arguments) {
      // This method is offending, if it has a sensible variable, WARN
      LOGGER.debug("Just found offending method call");
      doesStatementLeak = someValueApplies(arguments, new ContainsSensibleVariableVisitor(localsSensibility, mainClass));
    }
  }

}
