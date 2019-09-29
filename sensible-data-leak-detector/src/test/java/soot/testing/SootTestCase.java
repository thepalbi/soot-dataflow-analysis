package soot.testing;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public abstract class SootTestCase {

  private static final String TEST_PHASE_NAME = "jtp.test";
  private static final String JTP = "jtp";
  protected final Logger LOGGER = getLogger(SootTestCase.class);
  private List<Transform> transformsToRegister = new LinkedList<>();

  @Before
  public void setUp() throws Exception {

    // Register all user transformations
    transformsToRegister.stream().forEach(transform -> PackManager.v().getPack(JTP).add(transform));

    PackManager.v().getPack(JTP).add(new Transform(TEST_PHASE_NAME, new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String s, Map<String, String> map) {
        doInternalTransform(body, s, map);
      }
    }));

    Options.v()
        .set_soot_classpath(
                            "/Users/pbalbi/Facultad/aap/repo/sensible-data-leak-detector/target/test-classes:" +
                                "/Users/pbalbi/Facultad/aap/repo/sensible-data-leak-detector/target/classes");
    Options.v().set_prepend_classpath(true);
    Options.v().set_output_format(Options.output_format_jimple);
    Options.v().set_keep_line_number(true);
    PhaseOptions.v().setPhaseOption("jtp.test", "on");
  }

  @After
  public void tearDown() throws Exception {
    transformsToRegister.clear();
  }

  public void runSootForTargetClass(String fullyQualifiedName) {
    Options.v().classes().add(fullyQualifiedName);
    doRunSoot();
  }

  private void doRunSoot() {
    Scene.v().loadNecessaryClasses();
    PackManager.v().runPacks();
  }

  protected void doInternalTransform(Body body, String s, Map<String, String> map) {};

  public void addTransformToJtpPipeline(String phaseName, BodyTransformer transformer) {
    transformsToRegister.add(new Transform(phaseName, transformer));
    PhaseOptions.v().setPhaseOption(phaseName, "on");
  }

}
