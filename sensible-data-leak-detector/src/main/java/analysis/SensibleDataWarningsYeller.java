package analysis;

import org.slf4j.Logger;
import soot.Body;
import soot.BodyTransformer;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static soot.UnitUtils.getLineNumberFromUnit;

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

}
