import java.util.Map;

import dataflow.ZeroAnalysis;
import dataflow.abs.ZeroLattice;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Launcher {

  public static void main(String[] args) {
    PackManager.v().getPack("jtp").add(new Transform("jtp.ZeroAnalysis", new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        ZeroAnalysis results = new ZeroAnalysis(new ExceptionalUnitGraph(body));
        for (Unit unit : body.getUnits()) {
          ZeroLattice resolvedValue = results.resolvedForUnit.get(unit);
          if (resolvedValue != null) {
            unit.addTag(new StringTag("Resolved value for unit: " + resolvedValue.toString()));
          }
        }
      }
    }));
    soot.Main.main(args);
  }
}
