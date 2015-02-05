package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.RepetitionGenerator;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a uniformly distributed random sample of values.
 */
public class UniformDistributionGenerator extends RepetitionGenerator<BigDecimal> {
  private final BigDecimal lowerBound;
  private final BigDecimal upperBound;
  private final int count;
  private final SecureRandom secureRandom;

  /**
   * Constructor.
   *
   * @param key the key to which the generated values will be assigned
   * @param lowerBound the lower bound for the range (inclusive)
   * @param upperBound the upper bound for the range (exclusive)
   * @param count the number of values to generate
   */
  public UniformDistributionGenerator(String key, BigDecimal lowerBound, BigDecimal upperBound, int count) {
    super(key);
    this.lowerBound = lowerBound;
    this.count = count;
    this.upperBound = upperBound;
    secureRandom = new SecureRandom();
  }

  @Override
  public List<BigDecimal> generate() {
    List<BigDecimal> list = new ArrayList<BigDecimal>();
    BigDecimal range = upperBound.subtract(lowerBound);
    for (int i = 0; i < count; ++i) {
      list.add(new BigDecimal(secureRandom.nextDouble()).multiply(range).add(lowerBound));
    }
    return list;
  }

  public int getCount() {
    return count;
  }

  public BigDecimal getLowerBound() {
    return lowerBound;
  }

  public BigDecimal getUpperBound() {
    return upperBound;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UniformDistributionGenerator that = (UniformDistributionGenerator) o;

    if (count != that.count) return false;
    if (!lowerBound.equals(that.lowerBound)) return false;
    if (!upperBound.equals(that.upperBound)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = lowerBound.hashCode();
    result = 31 * result + upperBound.hashCode();
    result = 31 * result + count;
    return result;
  }
}
