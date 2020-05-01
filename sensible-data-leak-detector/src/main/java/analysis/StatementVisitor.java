package analysis;


import analysis.abstraction.SensibilityLattice;
import dataflow.utils.ValueVisitor;
import heros.solver.Pair;
import org.slf4j.Logger;
import soot.*;
import soot.jimple.*;
import wtf.thepalbi.PointsToResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static analysis.abstraction.SensibilityLattice.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Visitor for extracting from {@link Stmt} whether or not a sensible value is leaked.
 */
public class StatementVisitor {

    private final Logger LOGGER = getLogger(StatementVisitor.class);

    /**
     * Mapping from local variables to their sensibility level.
     */
    private Map<String, SensibilityLattice> localsSensibility;

    /**
     * Parameters of the method of which this statement belongs.
     */
    private Map<Integer, SensibilityLattice> params;

    private SootMethod inMethod;
    private PointsToResult pointsTo;
    private Boolean returningSensibleValue = false;
    private Boolean doesStatementLeak = false;
    private Stmt statement;

    public StatementVisitor(Map<String, SensibilityLattice> localsSensibility, Map<Integer, SensibilityLattice> params,
                            SootMethod method, PointsToResult pointsTo) {
        // TODO: Wrap all other visitor settings in a context object, since it's passed around
        this.localsSensibility = localsSensibility;
        this.params = params;
        this.inMethod = method;
        this.pointsTo = pointsTo;
    }

    public StatementVisitor visit(Stmt statement) {
        // TODO: Move statement to visitor constructor, since it's a method Object
        this.statement = statement;
        if (statement instanceof AssignStmt) {
            // Just handle AssignmentStmt. Identity type statements do not influence in this analysis
            AssignStmt assignStmt = (AssignStmt) statement;
            visitAssignment(assignStmt);
        } else if (statement instanceof InvokeStmt) {
            InvokeStmt invoke = (InvokeStmt) statement;
            visitInvoke(invoke, invoke.getInvokeExpr());
        } else if (statement instanceof ReturnStmt) {
            visitReturn();
        } else {
            LOGGER.debug("Ignoring statement with text: '{}' and class '{}'", statement.toString(), statement.getClass().getName());
        }
        return this;
    }

    private void visitReturn() {
        // Assuming that the returned value from the statement is always a Local or Field (eg. not a invocation)
        // Checking useBoxes fro sensible returns
        for (ValueBox valueBox : statement.getUseBoxes()) {
            Value someReturnedValue = valueBox.getValue();
            if (someReturnedValue instanceof Local && isLocalSensible((Local) someReturnedValue)) {
                returningSensibleValue = true;
                break;
            }
        }
    }

    private boolean isLocalSensible(Local local) {
        return SensibilityLattice.isSensible(localsSensibility.get(AssigneeNameExtractor.from(local)));
    }

    /**
     * Visits a standalone method invocation. This method just checks for special edge cases, and if its a regular
     * invoke dispatches to {@link StatementVisitor#visitRegularInvoke(InvokeStmt, InvokeExpr)};
     *
     * @param invoke     the invocation statement
     * @param invokeExpr the invocation expression
     */
    private void visitInvoke(InvokeStmt invoke, InvokeExpr invokeExpr) {
        SootMethodRef invokedMethod = invokeExpr.getMethodRef();
        List<Value> arguments = invokeExpr.getArgs();
        if (methodIdentifiedBy(invokedMethod, "analysis.SensibilityMarker", "markAsSensible")) {
            assert arguments.size() == 1;
            localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), HIGH);
        } else if (methodIdentifiedBy(invokedMethod, "analysis.SensibilityMarker", "sanitize")) {
            assert arguments.size() == 1;
            localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), NOT_SENSIBLE);
        } else if (DoesMethodLeak.check(invokeExpr, localsSensibility)) {
            doesStatementLeak = true;
        } else {
            visitRegularInvoke(invoke, invokeExpr);
        }
    }

    // TODO: Read about how Soot handles interprocedular dataflow analysis. In here I think I'm doing everything for myself.

    /**
     * Visits a regular method invocation, which might require to run a separate analysis on the called method, given
     * this context of sensibility.
     *
     * @param invoke     the invoke statement
     * @param invokeExpr the invoke expression
     */
    private void visitRegularInvoke(InvokeStmt invoke, InvokeExpr invokeExpr) {
        if (invokeExpr instanceof InterfaceInvokeExpr) {
            // Handle interface invoke
        } else {
            // Handle defined invoke
        }
    }

    private void visitAssignment(AssignStmt assignStmt) {
        if (assignStmt.getLeftOp() instanceof FieldRef) {
            // TODO: Handle field assignment
            LOGGER.warn("Assignment to fields not supported yet: {}", assignStmt);
        }

        Value rightOp = assignStmt.getRightOp();

        // Visiting the right operand of the assignment can have three outcomes:
        // - The right operand resolves to a value that is sensible (by directly being one, or an operation between
        //   some of them), hence the assignee becomes sensible
        // - The right operand consists of an invocation, which does not return a sensible value, but as side effects
        //   it leaks a sensible value. // TODO: Check if this is supported
        // - The right operand resolves to a non-sensible value. Update locals sensibility value.

        if (rightOp instanceof InvokeExpr) {
            // This is an assignment from an expression returned value

        } else {
            // The rightOp might me a field, or some operation over useBoxes
            // Leverage this in a naive way
            for (ValueBox valueBox : rightOp.getUseBoxes()) {
                Value rightUseValue = valueBox.getValue();
                if (rightUseValue instanceof Local && isLocalSensible((Local) rightUseValue)) {
                    // If some of the use boxes in the right expression is sensible, make result sensible to be MAY (in a coarse way)
                    localsSensibility.put(
                            AssigneeNameExtractor.from(assignStmt.getLeftOp()),
                            HIGH);
                    break;
                }
            }
        }
    }

    private boolean methodIdentifiedBy(SootMethodRef method, String fqClassName, String methodName) {
        return method.getDeclaringClass().getName().equals(fqClassName)
                && method.getName().equals(methodName);

    }

    public static Map<Integer, SensibilityLattice> getArgumentSensibilityFor(Map<String, SensibilityLattice> locals,
                                                                             List<Value> arguments) {
        AtomicInteger index = new AtomicInteger(0);
        return arguments.stream()
                .map(value -> new Pair<>(index.getAndIncrement(), value))
                .filter(paramPair -> paramPair.getO2() instanceof Local)
                .map(paramPair -> new Pair<>(paramPair.getO1(),
                        locals.getOrDefault(((Local) paramPair.getO2()).getName(),
                                BOTTOM)))
                .collect(Collectors.toMap(pair -> pair.getO1(), pair -> pair.getO2()));
    }

    public Boolean getReturningSensibleValue() {
        return returningSensibleValue;
    }

    public Boolean doesStatementLeak() {
        return doesStatementLeak;
    }

    private static class DoesMethodLeak {
        // TODO: Check methods that may leak its arguments here (eg. println, etc.).
        // Notice that some of them might be static method calls.
        public static boolean check(InvokeExpr invokeExpr, Map<String, SensibilityLattice> sensibilityValues) {
            if (invokeExpr.getMethod().getDeclaringClass().getName().equals("java.io.PrintStream") &&
                    invokeExpr.getMethod().getName().equals("println") &&
                    SensibilityLattice.isSensible(sensibilityValues.get(AssigneeNameExtractor.from(invokeExpr.getArg(0))))) {
                return true;
            }
            return false;
        }
    }
}

