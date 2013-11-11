package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.RepetitionGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sequence of decimal numbers beginning at start, with a step-size of stepSize,
 * and "count" elements.
 */
public class LinearStepGenerator extends RepetitionGenerator<BigDecimal> {
  private final BigDecimal start;
  private final int count;
  private final BigDecimal stepSize;

  public LinearStepGenerator(String key, BigDecimal start, int count, BigDecimal stepSize) {
    super(key);
    this.start = start;
    this.count = count;
    this.stepSize = stepSize;
  }

  @Override
  public List<BigDecimal> generate() {
    List<BigDecimal> results = new ArrayList<BigDecimal>();
    for (int i = 0; i < count; ++i) {
      results.add(start.add(stepSize.multiply(BigDecimal.valueOf(i))));
    }
    return results;
  }
}
