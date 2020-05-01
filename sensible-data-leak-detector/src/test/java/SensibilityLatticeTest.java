import analysis.abstraction.SensibilityLattice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static analysis.abstraction.SensibilityLattice.*;

public class SensibilityLatticeTest {

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(NOT_SENSIBLE, HIGH, MAYBE_SENSIBLE),
                Arguments.of(NOT_SENSIBLE, NOT_SENSIBLE, NOT_SENSIBLE),
                Arguments.of(NOT_SENSIBLE, MAYBE_SENSIBLE, MAYBE_SENSIBLE),
                Arguments.of(NOT_SENSIBLE, BOTTOM, NOT_SENSIBLE)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testSupreme(SensibilityLattice operator1, SensibilityLattice operator2, SensibilityLattice expected) {
        Assertions.assertEquals(expected, supremeBetween(operator1, operator2));
    }
}
