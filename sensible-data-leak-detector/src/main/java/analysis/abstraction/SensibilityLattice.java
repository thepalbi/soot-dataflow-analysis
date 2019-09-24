package analysis.abstraction;

import javafx.util.Pair;

public enum SensibilityLattice {
  BOTTOM(0, 0), LOW(1, 0), HIGH(1, 1), MAYBE_SENSIBLE(3, 0);

  private final ComparablePair priority;

  SensibilityLattice(Integer priority, Integer innerPriority) {
    this.priority = new ComparablePair(priority, innerPriority);
  }

  public static SensibilityLattice supremeBetween(SensibilityLattice v1, SensibilityLattice v2) {
    if (v1.equals(v2)) {
      return v1;
    } else if (v1.priority.getKey().equals(v2.priority.getKey())) {
      return MAYBE_SENSIBLE;
    } else {
      return v1.compareTo(v2) == 1 ? v1 : v2;
    }
  }

  public static SensibilityLattice getBottom() {
    return BOTTOM;
  }

  public static SensibilityLattice getTop() {
    return MAYBE_SENSIBLE;
  }

  private class ComparablePair extends Pair<Integer, Integer> implements Comparable {

    /**
     * Creates a new pair
     *
     * @param key The key for this pair
     * @param value The value to use for this pair
     */
    public ComparablePair(Integer key, Integer value) {
      super(key, value);
    }

    @Override
    public int compareTo(Object o) {
      ComparablePair otherAsPair = (ComparablePair) o;
      if (this.getKey().equals(otherAsPair.getKey()) &&
          this.getValue().equals(otherAsPair.getValue())) {
        return 0;
      } else if (this.getKey() < otherAsPair.getKey() ||
          this.getValue() < otherAsPair.getValue()) {
        return -1;
      } else {
        return 1;
      }
    }
  }
}
