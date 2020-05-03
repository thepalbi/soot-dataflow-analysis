import dataflow.DivisionByZeroAnalysis;
import org.slf4j.Logger;
import soot.*;
import soot.tagkit.LineNumberTag;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class Launcher {

    public static void main(String[] args) {
        PackManager.v().getPack("jtp").add(new Transform("jtp.DivisionByZeroAnalysis", new BodyTransformer() {

            private final Logger LOGGER = getLogger(Launcher.class);

            @Override
            protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
                DivisionByZeroAnalysis results = new DivisionByZeroAnalysis(new ExceptionalUnitGraph(body));
                for (Unit unit : body.getUnits()) {
                    if (results.unitIsOffending(unit)) {
                        unit.getTags()
                                .stream()
                                .filter(tag -> tag instanceof LineNumberTag)
                                .map(tag -> (LineNumberTag) tag)
                                .findFirst()
                                .ifPresent(lineNumberTag -> {
                                    LOGGER.error("Found a possible division by zero in line {}", lineNumberTag.getLineNumber());
                                });
                        unit.addTag(new StringTag("Possible division by zero here"));
                    }
                }
            }
        }));
        soot.Main.main(args);
    }
}
