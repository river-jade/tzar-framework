package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Scenario;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Bean to represent the scenario configuration in the project config.
 */
public class ScenarioBean {
  private String name;
  private Map<String, Object> parameters;

  public static List<ScenarioBean> fromScenarios(List<Scenario> scenarios) {
    List<ScenarioBean> beans = Lists.newArrayList();
    for (Scenario scenario : scenarios) {
      beans.add(ScenarioBean.fromScenario(scenario));
    }
    return beans;
  }

  public static ScenarioBean fromScenario(Scenario scenario) {
    ScenarioBean bean = new ScenarioBean();
    bean.name = scenario.getName();
    bean.parameters = scenario.getParameters().asMap();
    return bean;
  }

  public static List<Scenario> toScenarios(List<ScenarioBean> beans) {
    if (beans == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<Scenario> scenarios = ImmutableList.builder();
    for (ScenarioBean bean : beans) {
      scenarios.add(bean.toScenario());
    }
    return scenarios.build();
  }

  private Scenario toScenario() {
    return new Scenario(name, parameters == null ? Parameters.EMPTY_PARAMETERS :
        Parameters.createParameters(parameters));
  }
}
