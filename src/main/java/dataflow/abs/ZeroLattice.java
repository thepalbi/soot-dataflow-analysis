package dataflow.abs;

/**
 * Lattice used in the ZeroAnalysis.
 */
public enum ZeroLattice {

  BOTTOM("bottom"), NOT_ZERO("not-zero"), ZERO("zero"), MAYBE_ZERO("maybe-zero");

  private String name;

  @Override
  public String toString() {
    return this.name;
  }

  ZeroLattice(String name) {
    this.name = name;
  }
}
