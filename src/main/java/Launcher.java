import java.util.Map;

import dataflow.ZeroAnalysis;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Transform;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Launcher {

  public static void main(String[] args) {
    PackManager.v().getPack("jtp").add(new Transform("jtp.ZeroAnalysis", new BodyTransformer() {

      @Override
      protected void internalTransform(Body body, String s, Map<String, String> map) {
        new ZeroAnalysis(new ExceptionalUnitGraph(body));
      }
    }));
    soot.Main.main(args);
  }
}
