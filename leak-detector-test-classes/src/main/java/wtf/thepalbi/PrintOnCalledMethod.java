package wtf.thepalbi;

import analysis.SensibilityMarker;

public class PrintOnCalledMethod {

    public static void main(String[] args) {
        String sensibleShit = "holis";
        sensibleShit = sensibleShit + "b";
        SensibilityMarker.markAsSensible(sensibleShit);
        concatAWord(sensibleShit);
    }

    private static void concatAWord(String sensibleShit) {
        System.out.println(sensibleShit);
    }

}
