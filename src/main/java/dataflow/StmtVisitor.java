package dataflow;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import dataflow.abs.ZeroLatticeValueVisitor;
import dataflow.abs.ZeroLattice;
import org.slf4j.Logger;
import soot.Local;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;

public class StmtVisitor {

  private final Logger LOGGER = getLogger(StmtVisitor.class);

  private VariableToLatticeMap variables;

  public Optional<ZeroLattice> resolvedValueIfAssignment = empty();

  public StmtVisitor(VariableToLatticeMap variables) {
    this.variables = variables;
  }

  public void visit(Stmt stmt) {
    if (stmt instanceof DefinitionStmt) {
      DefinitionStmt definition = (DefinitionStmt) stmt;
      visitDefinition((Local) definition.getLeftOp(), new ZeroLatticeValueVisitor(variables).visit(definition.getRightOp()).done());
    }
  }

  private void visitDefinition(Local variable, ZeroLattice assignment) {
    variables.put(variable.getName(), assignment);
    resolvedValueIfAssignment = of(assignment);
  }

}
