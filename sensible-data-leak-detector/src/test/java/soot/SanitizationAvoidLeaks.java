package soot;

import analysis.SensibilityMarker;

public class SanitizationAvoidLeaks {

  public static void main(String[] args) {
    String data = "holis";
    data += " perro";
    SensibilityMarker.markAsSensible(data);
    data += " caca";
    SensibilityMarker.sanitize(data);
    System.out.println(data);
  }

}
