package au.edu.rmit.tzar.api;

import au.edu.rmit.tzar.parser.Repetitions;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 * A specification for a particular project. A project spec includes a list of scenarios, each of
 * which includes a parameter set (composed of a set of variables, a set of input files, and a set of
 * output files), and a set of base parameters which are common to all scenarios, but may be
 * overridden by values in each scenario specification.
 */
public class ProjectSpec {
  private final Parameters baseParams;
  private final List<Scenario> scenarios;
  private final String projectName;
  private final Repetitions repetitions;
  private String runnerClass;
  private String runnerFlags;

  public ProjectSpec(String projectName, String runnerClass, String runnerFlags, Parameters baseParams,
      List<Scenario> scenarios, Repetitions repetitions) {
    this.projectName = projectName;
    this.runnerClass = runnerClass;
    this.runnerFlags = runnerFlags;
    this.baseParams = baseParams;
    this.scenarios = scenarios;
    this.repetitions = repetitions;
  }

  public Parameters getBaseParams() {
    return baseParams;
  }

  public String getProjectName() {
    return projectName;
  }

  public Repetitions getRepetitions() {
    return repetitions;
  }

  public String getRunnerClass() {
    return runnerClass;
  }

  public String getRunnerFlags() {
    return runnerFlags;
  }

  /**
   * Gets the list of scenarios for this project specification, or null if there
   * are no scenarios defined.
   *
   * @return
   */
  public List<Scenario> getScenarios() {
    return scenarios;
  }

  /**
   * Validates that the project spec has been correctly initialised. Throws an exception if baseParams
   * or projectName are not set.
   *
   * @throws TzarException
   */
  public void validate() throws TzarException {
    List<String> errors = new ArrayList<String>();
    if (baseParams == null) {
      errors.add("Base parameters must be set.");
    }
    if (projectName == null) {
      errors.add("Project name must be set.");
    }
    if (runnerClass == null) {
      errors.add("Runner class must be set.");
    }
    if (!errors.isEmpty()) {
      throw new TzarException(String.format("Errors parsing project spec: [\n%s\n]", Joiner.on('\n').join(errors)));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectSpec that = (ProjectSpec) o;

    if (baseParams != null ? !baseParams.equals(that.baseParams) : that.baseParams != null) return false;
    if (scenarios != null ? !scenarios.equals(that.scenarios) : that.scenarios != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = baseParams != null ? baseParams.hashCode() : 0;
    result = 31 * result + (scenarios != null ? scenarios.hashCode() : 0);
    return result;
  }
}
