package soot.sensibility;

import static soot.UnitUtils.getLineNumberFromUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import analysis.SensibleDataAnalysis;
import analysis.SensibleDataWarningsYeller;
import org.junit.Before;
import org.junit.Test;
import soot.Body;
import soot.Unit;
import soot.testing.SootTestCase;

public class AnalysisIntegrationTestCase extends SootTestCase {

  private List<Integer> offendingLines = new LinkedList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    addTransformToJtpPipeline("jtp.testee", new SensibleDataWarningsYeller());
    offendingLines.clear();
    super.setUp();
  }

  @Override
  protected void doInternalTransform(Body body, String s, Map<String, String> map) {
    // Setup sensibility analysis
    SensibleDataAnalysis sensibilityAnalysis = SensibleDataAnalysis.forBody(body);
    // Collect "leaking" lines
    for (Unit unit : body.getUnits()) {
      if (sensibilityAnalysis.possibleLeakInUnit(unit)) {
        offendingLines.add(getLineNumberFromUnit(unit));
      }
    }
  }

  @Test
  public void simpleOneMethodProgramWithSensiblePrintLn() {
    runSootForTargetClass("soot.TestMain");
    assertThat(offendingLines.size(), is(2));
    assertThat(offendingLines, contains(is(11), is(13)));
  }

  @Test
  public void printLnOnMainMethod() {
    runSootForTargetClass("soot.SimpleInterprocedural");
    assertThat(offendingLines.size(), is(1));
    assertThat(offendingLines, contains(is(12)));
  }

  @Test
  public void printLnOnCalledMethod() {
    runSootForTargetClass("soot.PrintOnCalledMethod");
    assertThat(offendingLines.size(), is(1));
    // Note that the offending method is the method call itself
    assertThat(offendingLines, contains(is(11)));
  }

  @Test
  public void sensibleDataReturnedByKnownMethod() {
    runSootForTargetClass("soot.SensibleDataReturnedByKnownMethod");
    assertThat(offendingLines.size(), is(1));
    // Note that the offending method is the method call itself
    assertThat(offendingLines, contains(is(9)));
  }

  @Test
  public void sensibleDataReturnedByUnknownMethod() {
    runSootForTargetClass("soot.SensibleDataReturnedByUnknownMethod");
    assertThat(offendingLines.size(), is(1));
    // Note that the offending method is the method call itself
    assertThat(offendingLines, contains(is(14)));
  }

  @Test
  public void sensibleVariableIsNotLeakedAfterSanitize() {
    runSootForTargetClass("soot.SanitizationAvoidLeaks");
    assertThat(offendingLines, empty());
  }
}
