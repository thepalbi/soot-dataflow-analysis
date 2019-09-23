import java.util.Map;

import dataflow.DivisionByZeroAnalysis;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Launcher {

  public static void main(String[] args) {
    PackManager.v().getPack("jtp").add(new Transform("jtp.DivisionByZeroAnalysis", new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        DivisionByZeroAnalysis results = new DivisionByZeroAnalysis(new ExceptionalUnitGraph(body));
        for (Unit unit : body.getUnits()) {
          if (results.unitIsOffending(unit)) {
            unit.addTag(new StringTag("Possible division by zero here"));
          }
        }
      }
    }));
    soot.Main.main(args);
  }
}
