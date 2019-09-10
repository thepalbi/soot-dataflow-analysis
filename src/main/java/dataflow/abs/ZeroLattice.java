package dataflow.abs;

import heros.JoinLattice;

public class ZeroLattice implements JoinLattice {

  public enum ZeroLatticeValues {
    BOTTOM, NOT_ZERO, ZERO, MAYBE_ZERO
  }

  public Object topElement() {
    return ZeroLatticeValues.MAYBE_ZERO;
  }

  public Object bottomElement() {
    return ZeroLatticeValues.BOTTOM;
  }

  public Object join(Object left, Object right) {
    return null;
  }
}
