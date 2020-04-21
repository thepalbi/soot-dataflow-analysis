package soot;

import analysis.SensibleDataAnalysis;
import org.junit.Test;
import soot.options.Options;
import sun.security.krb5.SCDynamicStoreConfig;
import wtf.thepalbi.PointToAnalysis;
import wtf.thepalbi.PointsToResult;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static soot.UnitUtils.getLineNumberFromUnit;

public class SensibilityWithPointsToIntegratedTestCase {
    private PointsToResult pointsTo;
    private List<Integer> offendingLines = new LinkedList<>();

    @Test
    public void test() throws Exception {
        configureCommonSootOptions();

        // First do a run, and calculate points to
        Scene.v().loadNecessaryClasses();
        PackManager.v().runBodyPacks();
        List<Body> targetBodies = Scene.v().getClasses().stream()
                .filter(sootClass -> sootClass.getPackageName().startsWith("wtf.thepalbi") && !sootClass.isInterface())
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
        // Register all user transformations
        PackManager.v().getPack("jtp").add(new Transform("jtp.test", new BodyTransformer() {

            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                // Just run analysis for target class
                if (!body.getMethod().getDeclaringClass().getName().equals("wtf.thepalbi.TestPointsToWithoutAnalysis")) {
                    return;
                }

                // Setup sensibility analysis
                SensibleDataAnalysis sensibilityAnalysis = SensibleDataAnalysis.forBodyAndParams(body, new HashMap<>(), pointsTo);
                // Collect "leaking" lines
                for (Unit unit : body.getUnits()) {
                    if (sensibilityAnalysis.possibleLeakInUnit(unit)) {
                        offendingLines.add(getLineNumberFromUnit(unit));
                    }
                }
            }
        }));

        // Turn on tested phase
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
        // System.out.println(offendingLines);
        assertThat(offendingLines, hasSize(1));
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
    }
}
