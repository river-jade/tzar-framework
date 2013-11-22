package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.Repetitions;
import au.edu.rmit.tzar.api.Scenario;
import au.edu.rmit.tzar.repository.CodeSourceImpl;

import java.util.List;
import java.util.Map;

/**
 * A specification for a particular project. A project spec includes a list of scenarios, each of
 * which includes a parameter set (composed of a set of variables, a set of input files, and a set of
 * output files), and a set of base parameters which are common to all scenarios, but may be
 * overridden by values in each scenario specification.
 */
public class ProjectSpecImpl implements ProjectSpec {
  private final Parameters baseParams;
  private final List<Scenario> scenarios;
  private final String projectName;
  private final Repetitions repetitions;
  private final Map<String, CodeSourceImpl> libraries;
  private String runnerClass;
  private String runnerFlags;

  public ProjectSpecImpl(String projectName, String runnerClass, String runnerFlags, Parameters baseParams,
      List<Scenario> scenarios, Repetitions repetitions, Map<String, CodeSourceImpl> libraries) {
    this.projectName = projectName;
    this.runnerClass = runnerClass;
    this.runnerFlags = runnerFlags;
    this.baseParams = baseParams;
    this.scenarios = scenarios;
    this.repetitions = repetitions;
    this.libraries = libraries;
  }

  @Override public Parameters getBaseParams() {
    return baseParams;
  }

  @Override public Map<String, CodeSourceImpl> getLibraries() {
    return libraries;
  }

  @Override public String getProjectName() {
    return projectName;
  }

  @Override public Repetitions getRepetitions() {
    return repetitions;
  }

  @Override public String getRunnerClass() {
    return runnerClass;
  }

  @Override public String getRunnerFlags() {
    return runnerFlags;
  }

  @Override public List<Scenario> getScenarios() {
    return scenarios;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectSpecImpl that = (ProjectSpecImpl) o;

    if (!baseParams.equals(that.baseParams)) return false;
    if (libraries != null ? !libraries.equals(that.libraries) : that.libraries != null) return false;
    if (!projectName.equals(that.projectName)) return false;
    if (repetitions != null ? !repetitions.equals(that.repetitions) : that.repetitions != null) return false;
    if (!runnerClass.equals(that.runnerClass)) return false;
    if (!runnerFlags.equals(that.runnerFlags)) return false;
    if (!scenarios.equals(that.scenarios)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = baseParams.hashCode();
    result = 31 * result + scenarios.hashCode();
    result = 31 * result + projectName.hashCode();
    result = 31 * result + (repetitions != null ? repetitions.hashCode() : 0);
    result = 31 * result + (libraries != null ? libraries.hashCode() : 0);
    result = 31 * result + runnerClass.hashCode();
    result = 31 * result + runnerFlags.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ProjectSpec{" +
        "baseParams=" + baseParams +
        ", scenarios=" + scenarios +
        ", projectName='" + projectName + '\'' +
        ", repetitions=" + repetitions +
        ", libraries=" + libraries +
        ", runnerClass='" + runnerClass + '\'' +
        ", runnerFlags='" + runnerFlags + '\'' +
        '}';
  }
}
