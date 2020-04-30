package analysis;


import analysis.abstraction.InvokeFunction;
import analysis.abstraction.SensibilityLattice;
import dataflow.utils.ValueVisitor;
import heros.solver.Pair;
import org.slf4j.Logger;
import soot.*;
import soot.jimple.*;
import wtf.thepalbi.HeapObject;
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

    private SootClass mainClass;
    private SootMethod inMethod;
    private PointsToResult pointsTo;
    private Boolean returningSensibleValue = false;
    private Boolean doesStatementLeak = false;

    public StatementVisitor(Map<String, SensibilityLattice> localsSensibility, Map<Integer, SensibilityLattice> params,
                            SootClass mainClass, SootMethod method, PointsToResult pointsTo) {
        this.localsSensibility = localsSensibility;
        this.params = params;
        this.mainClass = mainClass;
        this.inMethod = method;
        this.pointsTo = pointsTo;
    }

    public StatementVisitor visit(Stmt statement) {
        if (statement instanceof AssignStmt) {
            // Just handle AssignmentStmt. Identity type statements do not influence in this analysis
            AssignStmt assignStmt = (AssignStmt) statement;
            visitAssignment(assignStmt.getLeftOp(), assignStmt.getRightOp());
        } else if (statement instanceof InvokeStmt) {
            InvokeStmt invoke = (InvokeStmt) statement;
            visitInvoke(invoke, invoke.getInvokeExpr());
        } else if (statement instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) statement;
            visitReturn(returnStmt.getOp());
        } else {
            LOGGER.debug("Ignoring statement with text: '{}' and class '{}'", statement.toString(), statement.getClass().getName());
        }
        return this;
    }

    private void visitReturn(Value value) {
        returningSensibleValue = new ContainsSensibleVariableVisitor(localsSensibility, inMethod, pointsTo).visit(value).done();
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
        } else if (false) {
            // TODO: Check methods that may leak its arguments here (eg. println, etc.).
            // Notice that some of them might be static method calls.
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

    private void visitAssignment(Value assignee, Value value) {
        // TODO: Handle field assignment
        if (assignee instanceof Local) {
            // Visiting the right operand of the assignment can have three outcomes:
            // - The right operand resolves to a value that is sensible, hence the assignee becomes sensible
            // - The right operand consists of an invocation, which does not return a sensible value, but as side effects
            // it leaks a sensible value. // TODO: Check if this is supported
            // - The right operand resolves to a non-sensible value. Update locals sensibility value.

            // The `.done()` returns whether or not the resolved value is sensible. It does not indicate side effects
            SensibilityLattice resolvedLevel =
                    new ContainsSensibleVariableVisitor(localsSensibility, params, inMethod, pointsTo).visit(value).done() ? HIGH : NOT_SENSIBLE;
            localsSensibility.put(new AssigneeNameExtractor().visit(assignee).done(), resolvedLevel);
        } else {
            // Ignore, this would just affect if the right operand of the assignment is a method call, and it has side effects
            LOGGER.warn("Assignment to something other than locals is not handled: Assignee is of type {}", assignee.getClass().getName());
        }
    }

    public static boolean someValueApplies(List<Value> values, ValueVisitor<Boolean> booleanValueVisitor) {
        return values.stream()
                .map(argument -> booleanValueVisitor.visit(argument).done())
                .reduce(false, (soFar, currentIsSensible) -> soFar || currentIsSensible);

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

    public Boolean getDoesStatementLeak() {
        return doesStatementLeak;
    }

    private class LocalInvokeFunc implements InvokeFunction {

        @Override
        public boolean applies(InvokeStmt invocation, SootMethod method) {
            return method.getDeclaringClass().equals(mainClass);
        }

        @Override
        public void accept(InvokeStmt invocation, SootMethod method, List<Value> arguments) {
            // Maybe this method leaks some sensible variable. Run analysis on method
            // Collect params sensibility
            Map<Integer, SensibilityLattice> paramsSensibility = getArgumentSensibilityFor(localsSensibility, arguments);
            SensibleDataAnalysis calledMethodAnalysis =
                    SensibleDataAnalysis.forBodyAndParams(method.getActiveBody(), paramsSensibility, pointsTo);
            if (!calledMethodAnalysis.noLeaksDetected()) {
                doesStatementLeak = true;
            }

        }
    }

    private class OffenderInvokeFun implements InvokeFunction {

        public OffenderInvokeFun() {
        }

        @Override
        public boolean applies(InvokeStmt invocation, SootMethod method) {
            return new OffendingMethodPredicate().test(method);
        }

        @Override
        public void accept(InvokeStmt invocation, SootMethod method, List<Value> arguments) {
            // This method is offending, if it has a sensible variable, WARN
            LOGGER.debug("Just found offending method call");
            doesStatementLeak = someValueApplies(arguments, new ContainsSensibleVariableVisitor(localsSensibility, inMethod, pointsTo));
        }
    }

    private class InterfaceInvokeFunc implements InvokeFunction {

        @Override
        public boolean applies(InvokeStmt invocation, SootMethod method) {
            return invocation.getInvokeExpr() instanceof InterfaceInvokeExpr;
        }

        @Override
        public void accept(InvokeStmt invocation, SootMethod method, List<Value> arguments) {
            InterfaceInvokeExpr invokeExpr = (InterfaceInvokeExpr) invocation.getInvokeExpr();
            // Maybe this method leaks some sensible variable. Run analysis on method
            // Collect params sensibility
            Map<Integer, SensibilityLattice> paramsSensibility = getArgumentSensibilityFor(localsSensibility, arguments);

            // Resolve method body with points-to information
            List<HeapObject> heapObjects = pointsTo.localPointsTo(inMethod, ((Local) invokeExpr.getBase()).getName());
            String firstPointedObjectType = heapObjects.get(0).getType();
            SootMethod resolvedMethod = Scene.v().getSootClass(firstPointedObjectType).getMethod(method.getSubSignature());

            assert resolvedMethod.hasActiveBody();

            SensibleDataAnalysis calledMethodAnalysis =
                    SensibleDataAnalysis.forBodyAndParams(resolvedMethod.getActiveBody(), paramsSensibility, pointsTo);
            if (!calledMethodAnalysis.noLeaksDetected()) {
                doesStatementLeak = true;
            }
        }
    }
}
