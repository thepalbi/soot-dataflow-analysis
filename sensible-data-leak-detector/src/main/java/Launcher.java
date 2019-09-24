import java.util.Map;

import analysis.SensibleDataAnalysis;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Transform;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * Main analysis launcher class
 */
public class Launcher {

  public static void main(String[] args) {

    PackManager.v().getPack("jtp").add(new Transform("jtp.SensibleData", new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String s, Map<String, String> map) {
        SensibleDataAnalysis analysis = new SensibleDataAnalysis(new ExceptionalUnitGraph(body));

      }
    }));

    soot.Main.main(args);

  }

}
