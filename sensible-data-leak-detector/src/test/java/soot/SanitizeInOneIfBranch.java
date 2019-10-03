package soot;

import analysis.SensibilityMarker;

public class SanitizeInOneIfBranch {

  public static void main(String[] args) {
    String perro = "sam";
    perro += " es un perro";
    SensibilityMarker.markAsSensible(perro);
    int number = 10;
    if (number > 0) {
      perro += " holis";
    } else {
      SensibilityMarker.sanitize(perro);
    }
    System.out.println(perro);
  }

}
