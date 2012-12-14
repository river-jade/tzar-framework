package au.edu.rmit.tzar.api;

import java.io.File;
import java.util.Date;

/**
 * Represents a particular run of the framework.
 */
public class Run {
  private volatile int runId;

  private final String projectName;
  private final String scenarioName;

  private final String revision;
  private final String flags;

  private volatile String hostname;

  private volatile Date startTime;
  private volatile Date endTime;
  private final Parameters parameters;

  private volatile String state;
  private final String runset;
  private final String clusterName;

  private volatile File outputPath;
  private volatile String outputHost;
  private volatile String runnerClass;

  /**
   * Constructor.
   *
   * @param runId        the unique id for this run or null if the id is not yet known
   * @param projectName  the name of the project which defines this run
   * @param scenarioName the name of the scenario which defines this run or null if this run is without scenario
   * @param revision     the revision number of the model code to download and execute
   * @param flags        the flags to pass to the command to be executed
   * @param parameters   parameters for the run
   * @param state        execution state of the run
   * @param runset       name of a runset for this run
   * @param clusterName  name of the cluster to run upon
   * @param runnerClass  class to use to execute the run
   */
  public Run(Integer runId, String projectName, String scenarioName, String revision, String flags,
             Parameters parameters, String state, String runset, String clusterName, String runnerClass) {
    this.runnerClass = runnerClass;
    this.runId = runId == null ? -1 : runId;
    this.projectName = projectName;
    this.scenarioName = scenarioName;
    this.revision = revision;
    this.flags = flags;
    this.parameters = parameters;
    this.state = state;
    this.runset = runset;
    this.clusterName = clusterName;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getName() {
    return projectName + (scenarioName == null ? "" : "_" + scenarioName);
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public String getRevision() {
    return revision;
  }

  public String getFlags() {
    return flags;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public String getOutputHost() {
    return outputHost;
  }

  public void setOutputHost(String outputHost) {
    this.outputHost = outputHost;
  }

  public File getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(File outputPath) {
    this.outputPath = outputPath;
  }

  /**
   * @return the run id, or -1 if this run does not yet have an id.
   */
  public int getRunId() {
    return runId;
  }

  public void setRunId(int runId) {
    this.runId = runId;
  }

  public String getProjectName() {
    return projectName;
  }

  public String getScenarioName() {
    return scenarioName;
  }

  public String getRunset() {
    return runset;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getRunnerClass() {
    return runnerClass;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return "Run{" +
        "runId=" + runId +
        ", projectName='" + projectName + '\'' +
        ", scenarioName='" + scenarioName + '\'' +
        ", revision='" + revision + '\'' +
        ", flags='" + flags + '\'' +
        ", state='" + state + '\'' +
        ", runset='" + runset + '\'' +
        ", clustername='" + clusterName + '\'' +
        ", runnerclass='" + runnerClass + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Run run = (Run) o;

    if (runId != run.runId) return false;
    if (endTime != null ? !endTime.equals(run.endTime) : run.endTime != null) return false;
    if (flags != null ? !flags.equals(run.flags) : run.flags != null) return false;
    if (hostname != null ? !hostname.equals(run.hostname) : run.hostname != null) return false;
    if (outputHost != null ? !outputHost.equals(run.outputHost) : run.outputHost != null) return false;
    if (outputPath != null ? !outputPath.equals(run.outputPath) : run.outputPath != null) return false;
    if (parameters != null ? !parameters.equals(run.parameters) : run.parameters != null) return false;
    if (revision != null ? !revision.equals(run.revision) : run.revision != null) return false;
    if (projectName != null ? !projectName.equals(run.projectName) : run.projectName != null) return false;
    if (scenarioName != null ? !scenarioName.equals(run.scenarioName) : run.scenarioName != null) return false;
    if (runset != null ? !runset.equals(run.runset) : run.runset != null) return false;
    if (clusterName != null ? !clusterName.equals(run.clusterName) : run.clusterName != null) return false;
    if (startTime != null ? !startTime.equals(run.startTime) : run.startTime != null) return false;
    if (state != null ? !state.equals(run.state) : run.state != null) return false;
    if (runnerClass != null ? !runnerClass.equals(run.runnerClass) : run.runnerClass != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = runId;
    result = 31 * result + (projectName != null ? projectName.hashCode() : 0);
    result = 31 * result + (scenarioName != null ? scenarioName.hashCode() : 0);
    result = 31 * result + (revision != null ? revision.hashCode() : 0);
    result = 31 * result + (flags != null ? flags.hashCode() : 0);
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (runset != null ? runset.hashCode() : 0);
    result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 31 * result + (outputPath != null ? outputPath.hashCode() : 0);
    result = 31 * result + (outputHost != null ? outputHost.hashCode() : 0);
    result = 31 * result + (runnerClass != null ? runnerClass.hashCode() : 0);
    return result;
  }
}
