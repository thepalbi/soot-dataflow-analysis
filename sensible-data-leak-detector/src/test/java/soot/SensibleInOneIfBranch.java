package soot;

import analysis.SensibilityMarker;

public class SensibleInOneIfBranch {

  public static void main(String[] args) {
    String perro = "sam";
    perro += " es un perro";
    int number = 10;
    if (number > 0) {
      SensibilityMarker.markAsSensible(perro);
      perro += " holis";
    } else {
      SensibilityMarker.sanitize(perro);
    }
    System.out.println(perro);
  }
}
