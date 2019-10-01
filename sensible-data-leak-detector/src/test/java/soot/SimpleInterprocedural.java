package soot;

import analysis.SensibilityMarker;

public class SimpleInterprocedural {

  public static void main(String[] args) {
    String sensibleShit = "holis";
    sensibleShit = sensibleShit + "b";
    SensibilityMarker.markAsSensible(sensibleShit);
    String someOtherSensibleShit = concatAWord(sensibleShit);
    System.out.println(someOtherSensibleShit);
  }

  private static String concatAWord(String sensibleShit) {
    return sensibleShit + " holis";
  }

}
