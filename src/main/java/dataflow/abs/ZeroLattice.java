package dataflow.abs;

/**
 * Lattice used in the DivisionByZeroAnalysis.
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

  public ZeroLattice add(ZeroLattice another) {
    if (this.equals(NOT_ZERO) || another.equals(NOT_ZERO)) {
      return NOT_ZERO;
      // Neither is NOT_ZERO
    } else if (this.equals(MAYBE_ZERO) || another.equals(MAYBE_ZERO)) {
      return MAYBE_ZERO;
      // Neither is MAYBE_ZERO or NOT_ZERO
    } else {
      return ZERO;
    }
  }

  public ZeroLattice divideBy(ZeroLattice another) {
    if (another.equals(ZERO) || another.equals(MAYBE_ZERO)) {
      return MAYBE_ZERO;
      // another must be NOT_ZERO
    } else {
      return this;
    }
  }

  public ZeroLattice multiplyBy(ZeroLattice another) {
    if (this.equals(ZERO) || another.equals(ZERO)) {
      return ZERO;
    } else if (this.equals(MAYBE_ZERO) || another.equals(MAYBE_ZERO)) {
      return MAYBE_ZERO;
    } else {
      return NOT_ZERO;
    }
  }

  public ZeroLattice substract(ZeroLattice another) {
    if (this.equals(NOT_ZERO) && another.equals(NOT_ZERO)) {
      return MAYBE_ZERO;
    } else if ((this.equals(NOT_ZERO) && another.equals(ZERO)) ||
        (this.equals(ZERO) && another.equals(NOT_ZERO))) {
      return NOT_ZERO;
    } else if (this.equals(ZERO) && another.equals(ZERO)) {
      return ZERO;
    } else {
      return MAYBE_ZERO;
    }
  }
}
