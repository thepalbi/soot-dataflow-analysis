package dataflow.abs;

import java.util.HashMap;

import static dataflow.abs.ZeroLattice.BOTTOM;

public class VariableToLatticeMap extends HashMap<String, ZeroLattice> {

    @Override
    public ZeroLattice get(Object key) {
        return getOrDefault(key, BOTTOM);
    }
}
