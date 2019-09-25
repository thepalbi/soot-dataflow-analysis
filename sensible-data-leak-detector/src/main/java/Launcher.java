import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import analysis.SensibleDataAnalysis;
import org.slf4j.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * Main analysis launcher class
 */
public class Launcher {

  private static final Logger LOGGER = getLogger(SensibleDataAnalysis.class);

  public static void main(String[] args) {

    PackManager.v().getPack("jtp").add(new Transform("jtp.SensibleData", new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String s, Map<String, String> map) {
        SensibleDataAnalysis analysis = new SensibleDataAnalysis(new ExceptionalUnitGraph(body));
        body.getUnits().stream()
            .filter(analysis::possibleLeakInUnit)
            .forEach(unit -> LOGGER.warn("Possible leak found in line {}",
                                         getLineNumberFromUnit(unit)));

      }
    }));

    soot.Main.main(args);

  }

  private static int getLineNumberFromUnit(Unit unit) {
    return unit.getTags().stream()
        .filter(tag -> tag instanceof LineNumberTag)
        .findFirst()
        .map(tag -> (LineNumberTag) tag)
        // If no line numbers configured, return dummy one
        .orElseGet(() -> new LineNumberTag(-1))
        .getLineNumber();
  }

}
