package dataflow.errors;

public class VisitorNotImplementedForType extends RuntimeException {

  public VisitorNotImplementedForType(String name) {
    super(name);
  }
}
