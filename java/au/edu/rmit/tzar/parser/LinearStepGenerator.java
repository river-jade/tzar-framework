package au.edu.rmit.tzar.parser;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sequence of decimal numbers beginning at start, with a step-size of stepSize,
 * and "count" elements.
 */
public class LinearStepGenerator extends RepetitionGenerator<BigDecimal> {
  @SerializedName("start")
  private final BigDecimal start;
  @SerializedName("count")
  private final int count;
  @SerializedName("step_size")
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
