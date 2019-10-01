package analysis;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import org.slf4j.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.tagkit.LineNumberTag;

public class SensibleDataWarningsYeller extends BodyTransformer {

  private static final Logger LOGGER = getLogger(SensibleDataWarningsYeller.class);

  @Override
  protected void internalTransform(Body body, String s, Map<String, String> map) {
    SensibleDataAnalysis analysis = SensibleDataAnalysis.forBody(body);
    body.getUnits().stream()
        .filter(analysis::possibleLeakInUnit)
        .forEach(unit -> LOGGER.warn("Possible leak found in line {}",
                                     getLineNumberFromUnit(unit)));

  }

  public static int getLineNumberFromUnit(Unit unit) {
    return unit.getTags().stream()
        .filter(tag -> tag instanceof LineNumberTag)
        .findFirst()
        .map(tag -> (LineNumberTag) tag)
        // If no line numbers configured, return dummy one
        .orElseGet(() -> new LineNumberTag(-1))
        .getLineNumber();
  }

}
