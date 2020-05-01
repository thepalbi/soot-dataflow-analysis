package analysis;


import analysis.InvocationVisitor.InvocationResult;
import analysis.abstraction.SensibilityLattice;
import org.slf4j.Logger;
import soot.Local;
import soot.SootMethodRef;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static analysis.abstraction.SensibilityLattice.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Visitor for extracting from {@link Stmt} whether or not a sensible value is leaked.
 */
public class StatementVisitor {

    private final Logger LOGGER = getLogger(StatementVisitor.class);
    private final SensibleDataAnalysis.Context ctx;

    private Boolean returningSensibleValue = false;
    private Boolean doesStatementLeak = false;
    private Stmt statement;

    public StatementVisitor(SensibleDataAnalysis.Context ctx, Stmt stmt) {
        this.ctx = ctx;
        this.statement = stmt;
    }

    public StatementVisitor visit() {
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
        return SensibilityLattice.isSensible(ctx.localsSensibility.get(AssigneeNameExtractor.from(local)));
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
            // Mark value as sensible invocation
            assert arguments.size() == 1;
            ctx.localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), HIGH);
        } else if (methodIdentifiedBy(invokedMethod, "analysis.SensibilityMarker", "sanitize")) {
            // Clean value sensibility level
            assert arguments.size() == 1;
            ctx.localsSensibility.put(new AssigneeNameExtractor().visit(arguments.get(0)).done(), NOT_SENSIBLE);
        } else if (DoesMethodLeak.check(invokeExpr, ctx.localsSensibility)) {
            // Check if there's a leak in the current invocation
            doesStatementLeak = true;
        } else {
            // Visit regular invoke
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
        doesStatementLeak |= new InvocationVisitor(ctx, invokeExpr).visit().leakInCall;
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
            InvocationResult result = new InvocationVisitor(ctx, (InvokeExpr) rightOp).visit();
            if (result.returnsSensibleValue) {
                ctx.localsSensibility.put(
                        AssigneeNameExtractor.from(assignStmt.getLeftOp()),
                        HIGH
                );
            }
        } else {
            // The rightOp might me a field, or some operation over useBoxes
            // Leverage this in a naive way
            for (ValueBox valueBox : rightOp.getUseBoxes()) {
                Value rightUseValue = valueBox.getValue();
                if (rightUseValue instanceof Local && isLocalSensible((Local) rightUseValue)) {
                    // If some of the use boxes in the right expression is sensible, make result sensible to be MAY (in a coarse way)
                    ctx.localsSensibility.put(
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
        Map<Integer, SensibilityLattice> parametersMap = new HashMap<>();
        for (int i = 0; i < arguments.size(); i++) {
            Value currentArgument = arguments.get(i);
            if (currentArgument instanceof Local) {
                parametersMap.put(i, locals.get(((Local) currentArgument).getName()));
            } else {
                parametersMap.put(i, getBottom());
            }
        }
        return parametersMap;
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

