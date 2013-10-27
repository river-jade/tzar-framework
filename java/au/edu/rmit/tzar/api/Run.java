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
  private final String runnerFlags;

  private volatile String hostname;

  private volatile Date startTime;
  private volatile Date endTime;
  private final Parameters parameters;

  private volatile String state;
  private final String runset;
  private final String clusterName;

  private volatile File remoteOutputPath;
  private volatile String outputHost;
  private volatile String runnerClass;

  /**
   * Constructor.
   */
  private Run(Builder builder) {
    this.runnerClass = builder.runnerClass;
    this.runId = builder.id;
    this.projectName = builder.projectName;
    this.scenarioName = builder.scenarioName;
    this.revision = builder.revision;
    this.runnerFlags = builder.runnerFlags;
    this.parameters = builder.parameters;
    this.state = builder.state;
    this.runset = builder.runset;
    this.clusterName = builder.clusterName;
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

  public String getRunnerFlags() {
    return runnerFlags;
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

  /**
   * The path where the results of this run are stored on the machine specified by getOutputHost().
   * @return
   */
  public File getRemoteOutputPath() {
    return remoteOutputPath;
  }

  /**
   * Sets the path where the results of this run are stored on the machine specified by getOutputHost().
   * @return
   */
  public void setRemoteOutputPath(File outputPath) {
    this.remoteOutputPath = outputPath;
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
        ", runnerFlags='" + runnerFlags + '\'' +
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
    if (runnerFlags != null ? !runnerFlags.equals(run.runnerFlags) : run.runnerFlags != null) return false;
    if (hostname != null ? !hostname.equals(run.hostname) : run.hostname != null) return false;
    if (outputHost != null ? !outputHost.equals(run.outputHost) : run.outputHost != null) return false;
    if (remoteOutputPath != null ? !remoteOutputPath.equals(run.remoteOutputPath) : run.remoteOutputPath != null) return false;
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
    result = 31 * result + (runnerFlags != null ? runnerFlags.hashCode() : 0);
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (runset != null ? runset.hashCode() : 0);
    result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 31 * result + (remoteOutputPath != null ? remoteOutputPath.hashCode() : 0);
    result = 31 * result + (outputHost != null ? outputHost.hashCode() : 0);
    result = 31 * result + (runnerClass != null ? runnerClass.hashCode() : 0);
    return result;
  }

  public static class Builder {
    private final String projectName;
    private final String scenarioName;
    private int id = -1;
    private String revision;
    private String runnerFlags;
    private Parameters parameters = Parameters.EMPTY_PARAMETERS;
    private String state = "scheduled";
    private String runset;
    private String clusterName;
    private String runnerClass;

    /**
     *
     * @param projectName  the name of the project which defines this run
     * @param scenarioName the name of the scenario which defines this run or null if this run is without scenario
     */
    public Builder(String projectName, String scenarioName) {
      this.projectName = projectName;
      this.scenarioName = scenarioName;
    }

    /*
     * @param id        the unique id for this run or null if the id is not yet known
     */
    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    /**
     * @param revision     the revision number of the model code to download and execute
     */
    public Builder setRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * @param runnerFlags  the flags to pass to the runner to be executed
     */
    public Builder setRunnerFlags(String runnerFlags) {
      this.runnerFlags = runnerFlags;
      return this;
    }

    /**
     * @param parameters   parameters for the run
     */
    public Builder setParameters(Parameters parameters) {
      this.parameters = parameters;
      return this;
    }

    /**
     * @param state        execution state of the run
     */
    public Builder setState(String state) {
      this.state = state;
      return this;
    }

    /**
     * @param runset       name of a runset for this run
     */
    public Builder setRunset(String runset) {
      this.runset = runset;
      return this;
    }

    /**
     * @param clusterName  name of the cluster to run upon
     */
    public Builder setClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    /**
     * @param runnerClass  class to use to execute the run
     */
    public Builder setRunnerClass(String runnerClass) {
      this.runnerClass = runnerClass;
      return this;
    }

    public Run build() {
      return new Run(this);
    }
  }
}
