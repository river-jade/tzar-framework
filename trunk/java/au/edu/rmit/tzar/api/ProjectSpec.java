package au.edu.rmit.tzar.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * A specification for a particular project. A project spec includes a list of scenarios, each of
 * which includes a parameter set (composed of a set of variables, a set of input files, and a set of
 * output files), and a set of base parameters which are common to all scenarios, but may be
 * overridden by values in each scenario specification.
 */
public class ProjectSpec {
  @SerializedName("base_params")
  private final Parameters baseParams;
  @SerializedName("scenarios")
  private final List<Scenario> scenarios;
  @SerializedName("project_name")
  private final String projectName;

  public ProjectSpec(String projectName, Parameters baseParams, List<Scenario> scenarios) {
    this.projectName = projectName;
    this.baseParams = baseParams;
    this.scenarios = scenarios;
  }

  public Parameters getBaseParams() {
    return baseParams;
  }

  public String getProjectName() {
    return projectName;
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
   * @throws RdvException
   */
  public void validate() throws RdvException {
    if (baseParams == null) {
      throw new RdvException("Base parameters must be set, either programatically, or in the json specification.");
    }
    if (projectName == null) {
      throw new RdvException("Project name must be set, either programatically, or in the json specification.");
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
