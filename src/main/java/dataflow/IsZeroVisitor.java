package dataflow;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import dataflow.abs.ValueVisitor;
import dataflow.abs.ZeroLattice;
import org.slf4j.Logger;
import soot.Local;
import soot.Unit;
import soot.jimple.DefinitionStmt;

public class IsZeroVisitor {

  private final Logger LOGGER = getLogger(IsZeroVisitor.class);

  private VariableToLatticeMap variables;

  public Optional<ZeroLattice> resolvedValueIfAssignment = empty();

  public IsZeroVisitor(VariableToLatticeMap variables) {
    this.variables = variables;
  }

  public void visit(Unit unit) {
    if (unit instanceof DefinitionStmt) {
      DefinitionStmt definition = (DefinitionStmt) unit;
      visitDefinition((Local) definition.getLeftOp(), new ValueVisitor(variables).visit(definition.getRightOp()).done());
    }
  }

  private void visitDefinition(Local variable, ZeroLattice assignment) {
    variables.put(variable.getName(), assignment);
    resolvedValueIfAssignment = of(assignment);
  }

}
