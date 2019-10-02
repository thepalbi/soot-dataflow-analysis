package analysis;

import dataflow.utils.AbstractValueVisitor;
import dataflow.utils.ValueVisitor;
import soot.Local;
import soot.Value;
import soot.jimple.ArrayRef;

public class AssigneeNameExtractor extends AbstractValueVisitor<String> {

  AssigneeNameExtractor() {
    this.nameBuilder = new StringBuilder();
  }

  AssigneeNameExtractor(StringBuilder nameBuilder) {
    this.nameBuilder = nameBuilder;
  }

  private StringBuilder nameBuilder;

  public static String from(Value assignee) {
    return new AssigneeNameExtractor().visit(assignee).done();
  }

  @Override
  protected void visitLocal(Local variable) {
    nameBuilder.append(variable.getName());
  }

  @Override
  protected void visitIntegerConstant(int value) {
    nameBuilder.append(value);
  }

  @Override
  protected void visitArrayRef(ArrayRef ref) {
    // Visit base of arrayRef, that's the name of the array itself
    new AssigneeNameExtractor(nameBuilder).visit(ref.getBase());
    // Concat the index of it, after a dot
    nameBuilder.append(".");
    new AssigneeNameExtractor(nameBuilder).visit(ref.getIndex());
  }

  @Override
  public String done() {
    if (nameBuilder.toString().equals("")) {
      throw new RuntimeException("No NAME extracted for value");
    }
    return nameBuilder.toString();
  }

  @Override
  public ValueVisitor<String> cloneVisitor() {
    return new AssigneeNameExtractor();
  }
}
