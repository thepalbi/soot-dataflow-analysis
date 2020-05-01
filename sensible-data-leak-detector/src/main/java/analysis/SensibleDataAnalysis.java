package analysis;

import analysis.abstraction.SensibilityLattice;
import org.slf4j.Logger;
import soot.*;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import wtf.thepalbi.PointsToResult;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.supremeBetween;
import static org.slf4j.LoggerFactory.getLogger;

// TODO: Maybe it would be nice for the analysis to keep in the dataflow a trace from where each sensible data was originated.

// TODO: Make this inter-procedural.

// TODO: Add sensibility levels.

// TODO: Add sanitization, and a larger collection of offending methods, even ones as VirtualInvokes, and possibly load all of them from a config file.

public class SensibleDataAnalysis extends ForwardFlowAnalysis<Unit, Map<String, SensibilityLattice>> {

    private final Logger LOGGER = getLogger(SensibleDataAnalysis.class);
    private final SootClass mainClass;

    /**
     * Results of the points to analysis run with this method as first reachable one.
     */
    private final PointsToResult pointsTo;
    private final SootMethod method;

    private Map<String, SensibilityLattice> startingLocalsMap;
    private Map<Unit, Boolean> possibleLeakInUnit;
    private boolean returningSensibleValue = false;

    public static SensibleDataAnalysis forBody(Body body) {
        return new SensibleDataAnalysis(new ExceptionalUnitGraph(body), new HashMap<>(), null);
    }

    /**
     * Creates a new {@link SensibleDataAnalysis} for the given body, and method params
     *
     * @param body
     * @param params   the method params sensibility map
     * @param pointsTo
     * @return
     */
    public static SensibleDataAnalysis forBodyAndParams(Body body, Map<Integer, SensibilityLattice> params, PointsToResult pointsTo) {
        return new SensibleDataAnalysis(new ExceptionalUnitGraph(body), params, pointsTo);
    }

    public SensibleDataAnalysis(ExceptionalUnitGraph graph, Map<Integer, SensibilityLattice> methodParams, PointsToResult pointsTo) {
        super(graph);

        this.startingLocalsMap = new HashMap<>();
        this.possibleLeakInUnit = new HashMap<>();
        Body methodBody = graph.getBody();
        this.mainClass = methodBody.getMethod().getDeclaringClass();
        this.method = methodBody.getMethod();

        // Analysis just handling locals in method
        // As starting point, save all locals as bottom
        for (Local variable : methodBody.getLocals()) {
            this.startingLocalsMap.put(variable.getName(), SensibilityLattice.getBottom());
        }

        // Modify locals value method params bindings (IdentityStmts), as per methodParams says
        for (int i = 0; i < method.getParameterCount(); i++) {
            Local toLocal = methodBody.getParameterLocal(i);
            this.startingLocalsMap.put(toLocal.getName(), methodParams.get(i));
        }

        // NOTE: Is this necessary?
        if (pointsTo == null) {
            Iterable<Body> targetBodies = Scene.v().getClasses().stream()
                    .filter(sootClass -> sootClass.getPackageName().startsWith(mainClass.getPackageName()))
                    // Filter interface, they do not have method bodies
                    .filter(sootClass -> !sootClass.isInterface())
                    .flatMap(sootClass -> sootClass.getMethods().stream())
                    .map(method -> method.getActiveBody())
                    .collect(Collectors.toList());
            try {
                pointsTo = new wtf.thepalbi.PointToAnalysis().run(targetBodies, methodBody, Scene.v());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        this.pointsTo = pointsTo;

        doAnalysis();
    }

    @Override
    protected void flowThrough(Map<String, SensibilityLattice> in, Unit unit,
                               Map<String, SensibilityLattice> out) {

        Context ctx = new Context(in, mainClass, method, pointsTo);
        StatementVisitor visitor = new StatementVisitor(ctx, (Stmt) unit).visit();

        possibleLeakInUnit.put(unit, visitor.doesStatementLeak());
        // Since a return statement is last in the CFG, it's not needed to prevent overwrites
        returningSensibleValue = visitor.getReturningSensibleValue();

        out.clear();
        out.putAll(in);
    }

    public boolean possibleLeakInUnit(Unit unit) {
        return possibleLeakInUnit.getOrDefault(unit, false);
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
        // May analysis
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

    /**
     * Object containing all the analysis-wide necessary variables.
     */
    public static class Context {
        public final Map<String, SensibilityLattice> localsSensibility;
        public final SootClass inClass;
        public final SootMethod inMethod;
        public final PointsToResult pointsToData;

        public Context(
                Map<String, SensibilityLattice> localsSensibility,
                SootClass inClass,
                SootMethod inMethod,
                PointsToResult pointsToData
        ) {

            this.localsSensibility = localsSensibility;
            this.inClass = inClass;
            this.inMethod = inMethod;
            this.pointsToData = pointsToData;
        }
    }
}
