package au.edu.rmit.tzar.api;

/**
 * A scenario is effectively a Run template. It contains a set of parameters
 * which override the base parameters within a project (which may contain
 * multiple scenarios).
 */
public class Scenario {
  public static final String DEFAULT_NAME = "default_scenario";

  private final String name;
  private final Parameters parameters;

  public Scenario(String name, Parameters overrideParameters) {
    this.name = name;
    this.parameters = overrideParameters;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Scenario scenario = (Scenario) o;

    if (name != null ? !name.equals(scenario.name) : scenario.name != null) return false;
    if (parameters != null ? !parameters.equals(scenario.parameters) : scenario.parameters != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Scenario{" +
        "name='" + name + '\'' +
        ", parameters=" + parameters +
        '}';
  }
}
