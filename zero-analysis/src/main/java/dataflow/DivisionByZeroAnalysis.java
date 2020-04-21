package dataflow;

import dataflow.abs.VariableToLatticeMap;
import dataflow.abs.ZeroLattice;
import soot.Local;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashMap;

/**
 * Division by zero analysis
 */
public class DivisionByZeroAnalysis extends ForwardFlowAnalysis<Unit, VariableToLatticeMap> {

    private VariableToLatticeMap localsAsLattice = new VariableToLatticeMap();
    private HashMap<Unit, Boolean> possibleDivisionByZero = new HashMap<>();

    public DivisionByZeroAnalysis(UnitGraph graph) {
        super(graph);

        // Starting variable to lattice map
        for (Local local : graph.getBody().getLocals()) {
            localsAsLattice.put(local.getName(), ZeroLattice.BOTTOM);
        }

        doAnalysis();
    }

    protected void flowThrough(VariableToLatticeMap in, Unit unit, VariableToLatticeMap out) {
        // Local all values from input
        out.clear();
        out.putAll(in);

        if (unit instanceof DefinitionStmt) {
            DefinitionStmt definition = (DefinitionStmt) unit;

            // Assume just local variables
            Local variable = (Local) definition.getLeftOp();
            ZeroLatticeValueVisitor visitor = new ZeroLatticeValueVisitor(in);
            ZeroLattice resolvedValue = visitor.visit(definition.getRightOp()).done();

            if (visitor.getPossibleDivisionByZero()) {
                possibleDivisionByZero.put(unit, true);
            }

            // Set in flowed values
            out.put(variable.getName(), resolvedValue);
        }
    }

    protected VariableToLatticeMap newInitialFlow() {
        VariableToLatticeMap newInstance = new VariableToLatticeMap();
        newInstance.putAll(localsAsLattice);
        return newInstance;
    }

    protected void merge(VariableToLatticeMap input1, VariableToLatticeMap input2, VariableToLatticeMap output) {
        output.clear();
        output.putAll(input1);

        // Merge input2 already defined variables by taking the supreme of both
        for (String variable : input2.keySet()) {
            ZeroLattice currentValue = output.get(variable);
            ZeroLattice supreme = currentValue.supreme(input2.get(variable));
            output.put(variable, supreme);
        }
    }

    protected void copy(VariableToLatticeMap source, VariableToLatticeMap dest) {
        dest.clear();
        dest.putAll(source);
    }

    public boolean unitIsOffending(Unit unit) {
        return possibleDivisionByZero.getOrDefault(unit, false);
    }

}
