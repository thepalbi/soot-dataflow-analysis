import static analysis.abstraction.SensibilityLattice.BOTTOM;
import static analysis.abstraction.SensibilityLattice.HIGH;
import static analysis.abstraction.SensibilityLattice.NOT_SENSIBLE;
import static analysis.abstraction.SensibilityLattice.MAYBE_SENSIBLE;
import static analysis.abstraction.SensibilityLattice.supremeBetween;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import analysis.abstraction.SensibilityLattice;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SensibilityLatticeTest {

  private SensibilityLattice operator1;
  private SensibilityLattice operator2;
  private SensibilityLattice expected;


  @Parameterized.Parameters(name = "{3}")
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {NOT_SENSIBLE, HIGH, MAYBE_SENSIBLE, "top is returned on same level sensibilites"},
        {NOT_SENSIBLE, NOT_SENSIBLE, NOT_SENSIBLE, "supreme of equal values"},
        {NOT_SENSIBLE, MAYBE_SENSIBLE, MAYBE_SENSIBLE, "top acts as sink"},
        {NOT_SENSIBLE, BOTTOM, NOT_SENSIBLE, "bottom acts as neuter"},
    });
  }

  public SensibilityLatticeTest(SensibilityLattice operator1, SensibilityLattice operator2, SensibilityLattice expected,
                                String name) {
    this.operator1 = operator1;
    this.operator2 = operator2;
    this.expected = expected;
  }

  @Test
  public void testSupreme() {
    assertThat(supremeBetween(operator1, operator2),
               is(expected));
  }
}
