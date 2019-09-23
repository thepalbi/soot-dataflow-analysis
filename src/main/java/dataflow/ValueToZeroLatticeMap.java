package dataflow;

import java.util.HashMap;

import dataflow.abs.ZeroLattice;
import soot.Value;

public class ValueToZeroLatticeMap extends HashMap<Value, ZeroLattice> {

  @Override
  public ZeroLattice get(Object key) {
    ZeroLattice storedMapping = super.get(key);
    if (storedMapping == null) {
      return ZeroLattice.BOTTOM;
    }
    return storedMapping;
  }
}
