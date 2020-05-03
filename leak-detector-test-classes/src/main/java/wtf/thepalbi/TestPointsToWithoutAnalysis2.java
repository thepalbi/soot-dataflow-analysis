package wtf.thepalbi;

import analysis.SensibilityMarker;

public class TestPointsToWithoutAnalysis2 {
    public static void main(String[] args) {
        String sensibleData = "holis";
        Object nowSensibleObject = new SensibleMarker().leakOrNotLeak(sensibleData);
        System.out.println(nowSensibleObject);
    }
}

class TestPointsToWithoutAnalysis3 {
    public static void main(String[] args) {
        String sensibleData = "holis";
        Object nowSensibleObject = new DummyReturner().leakOrNotLeak(sensibleData);
        System.out.println(nowSensibleObject);
    }
}

class DummyReturner implements SomeOtherInterface {
    @Override
    public Object leakOrNotLeak(Object data) {
        return data;
    }
}

class SensibleMarker implements SomeOtherInterface {
    @Override
    public Object leakOrNotLeak(Object data) {
        SensibilityMarker.markAsSensible(data);
        return data;
    }
}

interface SomeOtherInterface {
    public Object leakOrNotLeak(Object data);
}
