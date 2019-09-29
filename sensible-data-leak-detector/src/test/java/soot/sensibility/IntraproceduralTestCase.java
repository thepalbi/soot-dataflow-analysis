package soot.sensibility;

import java.util.Map;

import analysis.SensibleDataWarningsYeller;
import org.junit.Before;
import org.junit.Test;
import soot.Body;
import soot.testing.SootTestCase;

public class IntraproceduralTestCase extends SootTestCase {

  @Override
  @Before
  public void setUp() throws Exception {
    addTransformToJtpPipeline("jtp.testee", new SensibleDataWarningsYeller());
    super.setUp();
  }

  @Override
  protected void doInternalTransform(Body body, String s, Map<String, String> map) {}

  @Test
  public void test() {
    runSoot();
  }
}
