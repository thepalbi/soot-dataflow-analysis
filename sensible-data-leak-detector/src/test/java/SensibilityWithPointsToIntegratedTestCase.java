import analysis.SensibleDataAnalysis;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import soot.*;
import soot.options.Options;
import wtf.thepalbi.PointToAnalysis;
import wtf.thepalbi.PointsToResult;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static soot.UnitUtils.getLineNumberFromUnit;

public class SensibilityWithPointsToIntegratedTestCase {
    private PointsToResult pointsTo;
    private List<Integer> offendingLines = new LinkedList<>();

    @After
    public void tearDown() {
        offendingLines.clear();
    }

    @Test
    public void findLeakWhenImplementationPrintsSensibleValue() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.TestPointsToWithoutAnalysis");
        assertThat(offendingLines, hasSize(1));
    }

    @Test
    public void noLeakFoundWithDummyImplementation() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.TestPointsToWithoutAnalysisNotLeaking");
        // System.out.println(offendingLines);
        assertThat(offendingLines, empty());
    }

    @Test
    public void simpleOneMethodProgramWithSensiblePrintLn() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.TestMain");
        assertThat(offendingLines.size(), is(1));
        assertThat(offendingLines, contains(is(11)));
    }

    @Test
    public void printLnOnMainMethodAndSensibleDataModifiedInCalledMethod() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SimpleInterprocedural");
        assertThat(offendingLines.size(), is(1));
        assertThat(offendingLines, contains(is(12)));
    }

    @Test
    public void printLnOnCalledMethod() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.PrintOnCalledMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(11)));
    }

    @Test
    public void sensibleDataReturnedByKnownMethod() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SensibleDataReturnedByKnownMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(9)));
    }

    @Test
    @Ignore("Failing due to lack of array support")
    public void sensibleDataReturnedByUnknownMethod() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SensibleDataReturnedByUnknownMethod");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(14)));
    }

    @Test
    public void sensibleVariableIsNotLeakedAfterSanitize() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SanitizationAvoidLeaks");
        assertThat(offendingLines, empty());
    }

    @Test
    public void afterSanitizingInOneBranchLeakIsDetected() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SanitizeInOneIfBranch");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(17)));
    }

    @Test
    public void leakOnBothIfBranchesIsDetected() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.LeakOnBothIfBranches");
        assertThat(offendingLines.size(), is(2));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(12), is(14)));
    }

    @Test
    public void afterMarkingAsSensibleInOneIfBranchLeadsToLeak() throws Exception {
        runPointsToAndSootForClass("wtf.thepalbi.SensibleInOneIfBranch");
        assertThat(offendingLines.size(), is(1));
        // Note that the offending method is the method call itself
        assertThat(offendingLines, contains(is(17)));
    }

    private void runPointsToAndSootForClass(String targetFQClassName) throws Exception {
        configureCommonSootOptions();

        // First do a run, and calculate points to
        Scene.v().loadNecessaryClasses();
        PackManager.v().runBodyPacks();
        List<Body> targetBodies = Scene.v().getClasses().stream()
                .filter(sootClass -> sootClass.getPackageName().startsWith("wtf.thepalbi") && !sootClass.isInterface())
                .flatMap(sootClass -> sootClass.getMethods().stream())
                .map(method -> method.getActiveBody())
                .collect(Collectors.toList());
        PointsToResult result = new PointToAnalysis(Scene.v()).forClassesUnderPackage(
                "wtf.thepalbi",
                Scene.v().getSootClass(targetFQClassName).getMethodByName("main").getActiveBody());
        this.pointsTo = result;

        // Reset soot
        G.reset();

        configureCommonSootOptions();

        // Register all user transformations
        PackManager.v().getPack("jtp").add(new Transform("jtp.test", new BodyTransformer() {

            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                // Just run analysis for target class. If not, this will run for all classes in test-classes module.
                if (!body.getMethod().getDeclaringClass().getName().equals(targetFQClassName)) {
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
    }

    private void configureCommonSootOptions() {
        // Set as classpath both test and src classes

        String testSourcesPath =
                new File(
                        getClass().getProtectionDomain().getCodeSource().getLocation().getPath(),
                        "../../../leak-detector-test-classes/target/classes"
                ).getPath();

        Options.v().set_process_dir(Arrays.asList(testSourcesPath));

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
