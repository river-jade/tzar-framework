package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.RepetitionGenerator;
import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sequence of values selected from a normal distribution with the provided mean
 * and standard deviation.
 */
public class NormalDistributionGenerator extends RepetitionGenerator<BigDecimal> {
  private final BigDecimal mean;
  private final int count;
  private final BigDecimal stdDev;
  private final SecureRandom secureRandom;

  public NormalDistributionGenerator(String key, BigDecimal mean, int count, BigDecimal stdDev) {
    super(key);
    this.mean = mean;
    this.count = count;
    this.stdDev = stdDev;
    secureRandom = new SecureRandom();
  }

  @Override
  public List<BigDecimal> generate() {
    List<BigDecimal> list = new ArrayList<BigDecimal>();
    for (int i = 0; i < count; ++i) {
      list.add(new BigDecimal(secureRandom.nextGaussian()).multiply(stdDev).add(mean));
    }
    return list;
  }

  public BigDecimal getMean() {
    return mean;
  }

  public int getCount() {
    return count;
  }

  public BigDecimal getStdDev() {
    return stdDev;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mean, count, stdDev);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NormalDistributionGenerator other = (NormalDistributionGenerator) obj;
    return Objects.equal(this.mean, other.mean) && Objects.equal(this.count, other.count) && Objects.equal(this.stdDev, other.stdDev);
  }
}
