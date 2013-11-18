package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.RepetitionGenerator;
import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sequence of decimal numbers beginning at start, with a step-size of stepSize,
 * and "count" elements.
 */
public class LinearStepGenerator extends RepetitionGenerator<BigDecimal> {
  final BigDecimal start;
  final int count;
  final BigDecimal stepSize;

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

  @Override
  public int hashCode() {
    return Objects.hashCode(start, count, stepSize);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final LinearStepGenerator other = (LinearStepGenerator) obj;
    return Objects.equal(this.start, other.start) && Objects.equal(this.count, other.count) && Objects.equal(this.stepSize, other.stepSize);
  }
}
