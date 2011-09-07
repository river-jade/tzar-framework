package au.edu.rmit.tzar.api;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tests for the LinearStepGenerator.
 */
public class LinearStepGeneratorTest extends TestCase {
  private static final String KEY = "some_key";

  /**
   * Tests basic linear step generation
   */
  public void testLinearStep() {
    RepetitionGenerator<BigDecimal> generator = new LinearStepGenerator(KEY, BigDecimal.valueOf(12.3), 5,
        BigDecimal.valueOf(0.2));
    assertEquals(KEY, generator.getKey());
    List<BigDecimal> values = generator.generate();
    assertEquals(5, values.size());
    for (int i = 0; i < 5; i++) {
      assertEquals(12.3 + (i * 0.2), values.get(i).doubleValue(), 0.0001);
    }
  }

  /**
   * Tests linear step generation with a negative step value.
   */
  public void testLinearStepNegativeStep() {
    RepetitionGenerator<BigDecimal> generator = new LinearStepGenerator(KEY, BigDecimal.valueOf(12.3), 5,
        BigDecimal.valueOf(-0.2));
    assertEquals(KEY, generator.getKey());
    List<BigDecimal> values = generator.generate();
    assertEquals(5, values.size());
    for (int i = 0; i < 5; i++) {
      assertEquals(12.3 - (i * 0.2), values.get(i).doubleValue(), 0.0001);
    }
  }
}
