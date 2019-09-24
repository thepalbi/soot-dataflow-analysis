package dataflow.abs;

import static dataflow.abs.ZeroLattice.BOTTOM;

import java.util.HashMap;

public class VariableToLatticeMap extends HashMap<String, ZeroLattice> {

  @Override
  public ZeroLattice get(Object key) {
    return getOrDefault(key, BOTTOM);
  }
}
