package soot;

import analysis.SensibilityMarker;

public class TestPointsToWithoutAnalysis {
    public static void main(String[] args) {
        String sensibleData = "holis";
        sensibleData = sensibleData.replace("i", "u");
        SensibilityMarker.markAsSensible(sensibleData);
        SomeInterface someLeaker = new Leaker();
        someLeaker.leakOrNotLeak(sensibleData);
    }
}

class NotALeaker implements SomeInterface {
    @Override
    public void leakOrNotLeak(String sensibleData) {
        String notUsedString = new String(sensibleData);
        notUsedString.isEmpty();
    }
}

class Leaker implements SomeInterface {
    @Override
    public void leakOrNotLeak(String sensibleData) {
        System.out.println(sensibleData);
    }
}

interface SomeInterface {
    public void leakOrNotLeak(String sensibleData);
}
