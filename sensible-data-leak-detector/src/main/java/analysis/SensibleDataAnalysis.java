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
     * Parameters of the method whose body this analysis is running on.
     */
    private final Map<Integer, SensibilityLattice> methodParams;

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
        // TODO: Merge params in localSensibility by using IdentityStatements
        this.methodParams = methodParams;
        this.mainClass = graph.getBody().getMethod().getDeclaringClass();
        this.method = graph.getBody().getMethod();

        // Analysis just handling locals in method
        // As starting point, save all locals as bottom
        for (Local variable : graph.getBody().getLocals()) {
            this.startingLocalsMap.put(variable.getName(), SensibilityLattice.getBottom());
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
                pointsTo = new wtf.thepalbi.PointToAnalysis().run(targetBodies, graph.getBody(), Scene.v());
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

        StatementVisitor visitor = new StatementVisitor(in, methodParams, method, pointsTo).visit((Stmt) unit);

        possibleLeakInUnit.put(unit, visitor.doesStatementLeak());
        // Since a return statement is last in the CFG, it's not needed to prevent overwrites
        returningSensibleValue = visitor.getReturningSensibleValue();

        out.clear();
        out.putAll(in);
    }

    public boolean possibleLeakInUnit(Unit unit) {
        return possibleLeakInUnit.getOrDefault(unit, false);
    }

    public boolean noLeaksDetected() {
        // TODO: Change this. Its horrible
        return
                possibleLeakInUnit.isEmpty() ||
                        !possibleLeakInUnit.values().stream()
                                .reduce(false, (aBoolean, aBoolean2) -> aBoolean || aBoolean2);
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
}
