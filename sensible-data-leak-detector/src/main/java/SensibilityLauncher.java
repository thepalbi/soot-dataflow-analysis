import analysis.SensibleDataWarningsYeller;
import soot.PackManager;
import soot.Transform;

/**
 * Main analysis launcher class
 */
public class SensibilityLauncher {


    public static void main(String[] args) {

        PackManager.v().getPack("jtp").add(new Transform("jtp.SensibleData", new SensibleDataWarningsYeller()));
        soot.Main.main(args);

    }


}
