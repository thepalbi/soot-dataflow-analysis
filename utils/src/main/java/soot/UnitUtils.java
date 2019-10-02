package soot;

import soot.tagkit.LineNumberTag;

public class UnitUtils {

  public static int getLineNumberFromUnit(Unit unit) {
    return unit.getTags().stream()
        .filter(tag -> tag instanceof LineNumberTag)
        .findFirst()
        .map(tag -> (LineNumberTag) tag)
        // If no line numbers configured, return dummy one
        .orElseGet(() -> new LineNumberTag(-1))
        .getLineNumber();
  }
}
