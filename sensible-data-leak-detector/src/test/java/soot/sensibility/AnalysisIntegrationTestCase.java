package soot.sensibility;

import analysis.SensibleDataAnalysis;
import org.junit.Before;
import org.junit.Test;
import soot.Body;
import soot.Unit;
import soot.testing.SootTestCase;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static soot.UnitUtils.getLineNumberFromUnit;

public class AnalysisIntegrationTestCase extends SootTestCase {

    private List<Integer> offendingLines = new LinkedList<>();

    @Override
    @Before
    public void setUp() throws Exception {
        // addTransformToJtpPipeline("jtp.testee", new SensibleDataWarningsYeller());
        offendingLines.clear();
        super.setUp();
    }

    @Override
    protected void doInternalTransform(Body body, String s, Map<String, String> map) {
        // Setup sensibility analysis
        SensibleDataAnalysis sensibilityAnalysis = SensibleDataAnalysis.forBodyAndParams(body, new HashMap<>(), pointsTo);
        // Collect "leaking" lines
        for (Unit unit : body.getUnits()) {
            if (sensibilityAnalysis.possibleLeakInUnit(unit)) {
                offendingLines.add(getLineNumberFromUnit(unit));
            }
        }
    }

    @Test
    public void simpleOneMethodProgramWithSensiblePrintLn() {
        runSootForTargetClass("wtf.thepalbi.TestMain");
        assertThat(offendingLines.size(), is(2));
        assertThat(offendingLines, contains(is(11), is(13)));
    }

    @Test
    public void printLnOnMainMethod() {
        runSootForTargetClass("wtf.thepalbi.SimpleInterprocedural");
        assertThat(offendingLines.size(), is(1));
        assertThat(offendingLines, contains(is(12)));
    }

    @Test
    public void printLnOnCalledMethod() {
        runSootForTargetClass("wtf.thepalbi.PrintOnCalledMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(11)));
    }

    @Test
    public void sensibleDataReturnedByKnownMethod() {
        runSootForTargetClass("wtf.thepalbi.SensibleDataReturnedByKnownMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(9)));
    }

    @Test
    public void sensibleDataReturnedByUnknownMethod() {
        runSootForTargetClass("wtf.thepalbi.SensibleDataReturnedByUnknownMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(14)));
    }

    @Test
    public void sensibleVariableIsNotLeakedAfterSanitize() {
        runSootForTargetClass("wtf.thepalbi.SanitizationAvoidLeaks");
        assertThat(offendingLines, empty());
    }

    @Test
    public void afterSanitizingInOneBranchLeakIsDetected() {
        runSootForTargetClass("wtf.thepalbi.SanitizeInOneIfBranch");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(17)));
    }

    @Test
    public void leakOnBothIfBranchesIsDetected() {
        runSootForTargetClass("wtf.thepalbi.LeakOnBothIfBranches");
        assertThat(offendingLines.size(), is(2));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(12), is(14)));
    }

    @Test
    public void afterMarkingAsSensibleInOneIfBranchLeadsToLeak() {
        runSootForTargetClass("wtf.thepalbi.SensibleInOneIfBranch");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(17)));
    }

    @Test
    public void interfaceHandlingOnMethodInvokations() {
        runSootForTargetClass("wtf.thepalbi.TestPointsToWithoutAnalysis");
        assertThat(offendingLines, empty());
    }
}
