package soot;

import analysis.SensibilityMarker;

public class SensibleDataReturnedByKnownMethod {

  public static void main(String[] args) {
    String someSensibleData = getSensibleData();
    System.out.println(someSensibleData);
  }

  private static String getSensibleData() {
    String holis = "data";
    holis += " perro";
    SensibilityMarker.markAsSensible(holis);
    return holis;
  }

}
