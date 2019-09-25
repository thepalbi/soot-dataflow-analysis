package soot;

import analysis.SensibilityMarker;

public class TestMain {

  public static void main(String[] args) {
    String someSensibleData = "holis";
    someSensibleData = someSensibleData + "chauchis";
    SensibilityMarker.markAsSensible(someSensibleData);
    System.out.println(someSensibleData);
  }
}
