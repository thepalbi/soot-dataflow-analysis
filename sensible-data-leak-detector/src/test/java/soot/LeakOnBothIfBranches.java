package soot;

import static analysis.SensibilityMarker.markAsSensible;

public class LeakOnBothIfBranches {

  public static void main(String[] args) {
    String perro = "";
    perro += "sam";
    markAsSensible(perro);
    if (perro.length() > 0) {
      System.out.println(perro);
    } else {
      System.out.println(perro);
    }
  }

}
