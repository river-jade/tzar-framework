package au.edu.rmit.tzar.api;

import au.edu.rmit.tzar.repository.CodeSource;

import java.io.File;
import java.util.Date;

/**
 * Represents a particular run of the framework.
 */
public class Run {
  private final String projectName;
  private final String scenarioName;
  private final CodeSource codeSource;
  private volatile int runId = -1;
  private volatile String runnerFlags;
  private volatile String hostname;
  private volatile Date startTime;
  private volatile Date endTime;
  private volatile Parameters parameters = Parameters.EMPTY_PARAMETERS;
  private volatile String state = "scheduled";
  private volatile String runset = "default_runset";
  private volatile String clusterName;
  private volatile File remoteOutputPath;
  private volatile String outputHost;
  private volatile String runnerClass;

   /**
    * Constructor.
    * @param projectName  the name of the project which defines this run
    * @param scenarioName the name of the scenario which defines this run or null if this run is without scenario
    */
  public Run(String projectName, String scenarioName, CodeSource codeSource) {
    this.projectName = projectName;
    this.scenarioName = scenarioName;
    this.codeSource = codeSource;
  }

  public String getHostname() {
    return hostname;
  }

  public Run setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  public String getName() {
    return projectName + (scenarioName == null ? "" : "_" + scenarioName);
  }

  public Date getStartTime() {
    return startTime;
  }

  public Run setStartTime(Date startTime) {
    this.startTime = startTime;
    return this;
  }

  public Date getEndTime() {
    return endTime;
  }

  public Run setEndTime(Date endTime) {
    this.endTime = endTime;
    return this;
  }

  public CodeSource getCodeSource() {
    return codeSource;
  }

  public String getRunnerFlags() {
    return runnerFlags;
  }

  public Run setRunnerFlags(String runnerFlags) {
    this.runnerFlags = runnerFlags;
    return this;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public Run setParameters(Parameters parameters) {
    this.parameters = parameters;
    return this;
  }

  public String getOutputHost() {
    return outputHost;
  }

  public Run setOutputHost(String outputHost) {
    this.outputHost = outputHost;
    return this;
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
  public Run setRemoteOutputPath(File outputPath) {
    this.remoteOutputPath = outputPath;
    return this;
  }

  /**
   * @return the run id, or -1 if this run does not yet have an id.
   */
  public int getRunId() {
    return runId;
  }

  public Run setRunId(int runId) {
    this.runId = runId;
    return this;
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

  public Run setRunset(String runset) {
    this.runset = runset;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public Run setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public String getRunnerClass() {
    return runnerClass;
  }

  public Run setRunnerClass(String runnerClass) {
    this.runnerClass = runnerClass;
    return this;
  }

  public String getState() {
    return state;
  }

  public Run setState(String state) {
    this.state = state;
    return this;
  }

  @Override
  public String toString() {
    return "Run{" +
        "runId=" + runId +
        ", projectName='" + projectName + '\'' +
        ", scenarioName='" + scenarioName + '\'' +
        ", codeSource='" + codeSource + '\'' +
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
    if (codeSource != null ? !codeSource.equals(run.codeSource) : run.codeSource!= null) return false;
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
    result = 31 * result + (codeSource != null ? codeSource.hashCode() : 0);
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
}
