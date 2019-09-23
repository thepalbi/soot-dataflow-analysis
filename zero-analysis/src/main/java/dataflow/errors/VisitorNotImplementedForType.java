package dataflow.errors;

/**
 * No visitor implemented in {@link dataflow.utils.AbstractValueVisitor} for this {@link soot.Value} sub-type.
 */
public class VisitorNotImplementedForType extends RuntimeException {

  public VisitorNotImplementedForType(String name) {
    super(name);
  }
}
