package au.edu.rmit.tzar.parser;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sequence of values selected from a normal distribution with the provided mean
 * and standard deviation.
 */
public class NormalDistributionGenerator extends RepetitionGenerator<BigDecimal> {
  @SerializedName("mean")
  private final BigDecimal mean;
  @SerializedName("count")
  private final int count;
  @SerializedName("std_dev")
  private final BigDecimal stdDev;

  public NormalDistributionGenerator(String key, BigDecimal mean, int count, BigDecimal stdDev) {
    super(key);
    this.mean = mean;
    this.count = count;
    this.stdDev = stdDev;
  }

  @Override
  public List<BigDecimal> generate() {
    List<BigDecimal> list = new ArrayList<BigDecimal>();
    for (int i = 0; i < count; ++i) {
      list.add(new BigDecimal(new SecureRandom().nextGaussian()).multiply(stdDev).add(mean));
    }
    return list;
  }
}
