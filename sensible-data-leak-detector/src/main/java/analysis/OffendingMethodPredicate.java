package analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaml.snakeyaml.Yaml;
import soot.SootMethod;
import soot.jimple.spark.ondemand.genericutil.Predicate;

/**
 * Offending method {@link Predicate<SootMethod>} discovered from the configuration file.
 */
public class OffendingMethodPredicate extends Predicate<SootMethod> {

  private static final String OFFENDING_METHODS_CONFIG_FILE = "offending-methods.yaml";
  private static final String CLASS_FIELD = "class";
  private static final String METHOD_FIELD = "method";
  private static List<Predicate<SootMethod>> offendingMethodMatchers = new LinkedList<>();
  private static AtomicBoolean methodsAlreadyDiscovered = new AtomicBoolean(false);

  public OffendingMethodPredicate() {
    if (!methodsAlreadyDiscovered.getAndSet(true)) {
      // Methods not discovered yet
      Yaml offendingMethodsConfig = new Yaml();
      List<Map> loadedConfig =
          offendingMethodsConfig
              .load(Thread.currentThread().getContextClassLoader().getResourceAsStream(OFFENDING_METHODS_CONFIG_FILE));
      for (Map<String, String> config : loadedConfig) {
        String methodClass = config.get(CLASS_FIELD);
        String methodName = config.get(METHOD_FIELD);
        offendingMethodMatchers.add(new MethodPredicate(methodClass, methodName));
      }
    }
  }

  @Override
  public boolean test(SootMethod method) {
    for (Predicate<SootMethod> predicate : offendingMethodMatchers) {
      if (predicate.test(method))
        return true;
    }
    return false;
  }

  /**
   * {@link Predicate<SootMethod>} for matching a method according to its class name and method name.
   */
  private class MethodPredicate extends Predicate<SootMethod> {

    private String className;
    private String methodName;

    public MethodPredicate(String className, String methodName) {
      this.className = className;
      this.methodName = methodName;
    }

    @Override
    public boolean test(SootMethod method) {
      return method.getDeclaringClass().getName().equals(className) &&
          method.getName().equals(methodName);
    }
  }
}
