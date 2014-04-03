package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RepetitionGenerator;
import au.edu.rmit.tzar.api.Repetitions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean to represent the repetitions configuration in the project config.
 */
public class RepetitionsBean {
  private List<Map<String, Object>> static_repetitions;
  private List<RepetitionGeneratorBean> generators;

  public Repetitions toRepetitions() {
    List<Parameters> parametersList;
    if (static_repetitions == null) {
      parametersList = ImmutableList.of();
    } else {
      ImmutableList.Builder<Parameters> builder = ImmutableList.builder();
      for (Map<String, Object> bean : static_repetitions) {
        builder.add(Parameters.createParameters(bean));
      }
      parametersList = builder.build();
    }

    List<RepetitionGenerator<?>> repetitionGenerators;
    if (generators == null) {
      repetitionGenerators = ImmutableList.of();
    } else {
      ImmutableList.Builder<RepetitionGenerator<?>> builder = ImmutableList.builder();
      for (RepetitionGeneratorBean bean : generators) {
        builder.add(bean.toGenerator());
      }
      repetitionGenerators = builder.build();
    }
    return new Repetitions(parametersList, repetitionGenerators);
  }

  public static RepetitionsBean fromRepetitions(Repetitions repetitions) {
    RepetitionsBean bean = new RepetitionsBean();
    bean.static_repetitions = new ArrayList<Map<String, Object>>();
    for (Parameters parameters : repetitions.getStaticRepetitions()) {
      Map<String, Object> map = parameters.asMap();
      bean.static_repetitions.add(map);
    }
    bean.generators = new ArrayList<RepetitionGeneratorBean>();
    for (RepetitionGenerator<?> generator : repetitions.getGenerators()) {
      bean.generators.add(RepetitionGeneratorBean.fromGenerators(generator));
    }
    return bean;
  }
}
