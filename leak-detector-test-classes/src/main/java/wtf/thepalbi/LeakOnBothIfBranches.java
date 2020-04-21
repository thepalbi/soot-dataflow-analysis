package wtf.thepalbi;

import analysis.SensibilityMarker;

public class LeakOnBothIfBranches {

    public static void main(String[] args) {
        String perro = "";
        perro += "sam";
        SensibilityMarker.markAsSensible(perro);
        if (perro.length() > 0) {
            System.out.println(perro);
        } else {
            System.out.println(perro);
        }
    }

}
