package soot;

import analysis.SensibilityMarker;

public class SensibleDataReturnedByUnknownMethod {

  public static void main(String[] args) {
    String someSensibleData = "";
    someSensibleData += " holis";
    SensibilityMarker.markAsSensible(someSensibleData);

    String anotherVariable = String.format("esto es alto string y dice: %s", someSensibleData);

    System.out.println(anotherVariable);
  }
}
