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
  final BigDecimal mean;
  final int count;
  final BigDecimal stdDev;

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
