package dataflow;

import static dataflow.abs.ZeroLattice.BOTTOM;

import java.util.HashMap;

import dataflow.abs.ZeroLattice;

public class VariableToLatticeMap extends HashMap<String, ZeroLattice> {

  @Override
  public ZeroLattice get(Object key) {
    return getOrDefault(key, BOTTOM);
  }
}
