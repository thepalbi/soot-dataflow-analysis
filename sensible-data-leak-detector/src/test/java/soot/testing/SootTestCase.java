package soot.testing;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import soot.*;
import soot.options.Options;
import wtf.thepalbi.PointToAnalysis;
import wtf.thepalbi.PointsToResult;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class SootTestCase {

    private static final String TEST_PHASE_NAME = "jtp.test";
    private static final String JTP = "jtp";
    protected final Logger LOGGER = getLogger(SootTestCase.class);
    private List<Transform> transformsToRegister = new LinkedList<>();
    protected PointsToResult pointsTo;

    @Before
    public void setUp() throws Exception {
        configureCommonSootOptions();

        // First do a run, and calculate points to
        Scene.v().loadNecessaryClasses();
        PackManager.v().runBodyPacks();

        List<Body> targetBodies = Scene.v().getClasses().stream()
                .filter(sootClass -> sootClass.getPackageName().startsWith("soot") && !sootClass.isInterface())
                .flatMap(sootClass -> sootClass.getMethods().stream())
                .map(method -> method.getActiveBody())
                .collect(Collectors.toList());

        PointsToResult result = new PointToAnalysis().run(
                targetBodies,
                Scene.v().getSootClass("wtf.thepalbi.TestPointsToWithoutAnalysis").getMethodByName("main").getActiveBody(),
                Scene.v());
        this.pointsTo = result;

        // Reset soot
        G.reset();

        configureCommonSootOptions();

        // Turn on tested phase
        PhaseOptions.v().setPhaseOption("jtp.test", "on");

        // Register all user transformations
        transformsToRegister.stream().forEach(transform -> PackManager.v().getPack(JTP).add(transform));

        PackManager.v().getPack(JTP).add(new Transform(TEST_PHASE_NAME, new BodyTransformer() {

            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                doInternalTransform(body, s, map);
            }
        }));

    }

    private void configureCommonSootOptions() {
        // Set as classpath both test and src classes
        Options.v().set_process_dir(Arrays.asList(
                "/Users/thepalbi/Facultad/aap/soot-dataflow-analysis/leak-detector-test-classes/target/classes"
        ));

        // Use default JVM rt.jar
        Options.v().set_prepend_classpath(true);
        // Print tags in produced jimple
        Options.v().set_print_tags_in_output(true);
        // Use Jimple IR
        Options.v().set_output_format(Options.output_format_jimple);
        // Extract line-numbers from .class. NECESSARY
        Options.v().set_keep_line_number(true);

        Options.v().setPhaseOption("jb", "use-original-names: true");

        // Options.v().set_main_class("wtf.thepalbi.TestPointsToWithoutAnalysis");
    }

    @After
    public void tearDown() throws Exception {
        PackManager.v().getPack(JTP).remove(TEST_PHASE_NAME);
        transformsToRegister.stream()
                .forEach(transform -> PackManager.v().getPack(JTP).remove(transform.getPhaseName()));
        transformsToRegister.clear();
        G.reset();
    }

    public void runSootForTargetClass(String fullyQualifiedName) {
        Options.v().classes().add(fullyQualifiedName);
        doRunSoot();
    }

    private void doRunSoot() {
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }

    protected void doInternalTransform(Body body, String s, Map<String, String> map) {
    }

    ;

    public void addTransformToJtpPipeline(String phaseName, BodyTransformer transformer) {
        transformsToRegister.add(new Transform(phaseName, transformer));
        PhaseOptions.v().setPhaseOption(phaseName, "on");
    }

}
