package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.RepetitionGenerator;
import au.edu.rmit.tzar.parser.LinearStepGenerator;
import au.edu.rmit.tzar.parser.NormalDistributionGenerator;
import au.edu.rmit.tzar.parser.UniformDistributionGenerator;
import org.yaml.snakeyaml.error.YAMLException;

import java.math.BigDecimal;

/**
 * Bean to represent the Repetition generator in the project config.
 */
public class RepetitionGeneratorBean {
  private String generator_type;
  private String key;

  // number of values to generate
  private int count;

  // params for linear step generator
  private BigDecimal start;
  private BigDecimal step_size;

  // params for normal dist generator
  private BigDecimal mean;
  private BigDecimal std_dev;

  // params for uniform dist generator
  private BigDecimal lower_bound;
  private BigDecimal upper_bound;

  public RepetitionGenerator<?> toGenerator() {
    switch (RepetitionGenerator.GeneratorType.TYPES.get(generator_type)) {
      case LINEAR_STEP:
        return new LinearStepGenerator(key, start, count, step_size);
      case NORMAL_DISTRIBUTION:
        return new NormalDistributionGenerator(key, mean, count, std_dev);
      case UNIFORM_DISTRUBUTION:
        return new UniformDistributionGenerator(key, lower_bound, upper_bound, count);
      default:
        throw new YAMLException("Generator type: " + generator_type + " not recognised.");
    }
  }

  public static RepetitionGeneratorBean fromGenerators(RepetitionGenerator<?> generator) {
    Class<? extends RepetitionGenerator> generatorClass = generator.getClass();
    final RepetitionGenerator.GeneratorType type;
    RepetitionGeneratorBean bean = new RepetitionGeneratorBean();
    if (generatorClass == LinearStepGenerator.class) {
      type = RepetitionGenerator.GeneratorType.LINEAR_STEP;
      LinearStepGenerator linearStepGenerator = (LinearStepGenerator) generator;
      bean.count = linearStepGenerator.getCount();
      bean.start = linearStepGenerator.getStart();
      bean.step_size = linearStepGenerator.getStepSize();
    } else if (generatorClass == NormalDistributionGenerator.class) {
      type = RepetitionGenerator.GeneratorType.NORMAL_DISTRIBUTION;
      NormalDistributionGenerator normalDistributionGenerator = (NormalDistributionGenerator) generator;
      bean.count = normalDistributionGenerator.getCount();
      bean.mean = normalDistributionGenerator.getMean();
      bean.std_dev = normalDistributionGenerator.getStdDev();
    } else if (generatorClass == UniformDistributionGenerator.class) {
      type = RepetitionGenerator.GeneratorType.UNIFORM_DISTRUBUTION;
      UniformDistributionGenerator uniformDistributionGenerator = (UniformDistributionGenerator) generator;
      bean.count = uniformDistributionGenerator.getCount();
      bean.lower_bound = uniformDistributionGenerator.getLowerBound();
      bean.upper_bound = uniformDistributionGenerator.getUpperBound();
    } else {
      throw new YAMLException("Generator type: " + generatorClass + " not recognised.");
    }
    bean.generator_type = type.toString().toLowerCase();
    bean.key = generator.getKey();
    return bean;
  }
}
