package analysis;

import dataflow.utils.AbstractValueVisitor;
import dataflow.utils.ValueVisitor;
import soot.Local;
import soot.Value;
import soot.jimple.ArrayRef;

/**
 * {@link ValueVisitor} used to extract an assignee name inside a {@link soot.jimple.DefinitionStmt}.
 */
public class AssigneeNameExtractor extends AbstractValueVisitor<String> {

    private AssigneeNameExtractor() {
        this.nameBuilder = new StringBuilder();
    }

    private AssigneeNameExtractor(StringBuilder nameBuilder) {
        this.nameBuilder = nameBuilder;
    }

    private StringBuilder nameBuilder;

    /**
     * Extract the name of the assignee inside a {@link soot.jimple.DefinitionStmt}.
     *
     * @param assignee the assignee
     * @return the resolved name
     */
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
        // Previously I was concatenating the accessed index with the array
        // name, but it's not worth since in that case I should be able to
        // correlate base variable with all assigned indexes. Not worth!
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
