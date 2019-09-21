package dataflow;

import java.util.HashMap;

import soot.IntegerType;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;

public class IsZeroVisitor {

  public BooleanMap<Value> varaibleIsInteger = new BooleanMap<>();

  public void visitDefinition(DefinitionStmt definition) {
    Value variable = definition.getLeftOp();
    if (definition.getRightOp().getType() instanceof IntegerType) {
      varaibleIsInteger.set(variable);
    } else if (varaibleIsInteger.get(variable)) {
      varaibleIsInteger.clear(variable);
    }
  }

  public void visit(Unit unit) {
    if (unit instanceof DefinitionStmt) {
      this.visitDefinition((DefinitionStmt) unit);
    }

  }

  class BooleanMap<K> extends HashMap<K, Boolean> {

    public void clear(K key) {
      this.put(key, false);
    }

    public void set(K key) {
      this.put(key, true);
    }

    @Override
    public Boolean get(Object key) {
      return this.getOrDefault(key, false);
    }
  }

}
