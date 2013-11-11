package au.edu.rmit.tzar.api;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.base.Objects;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Represents a particular run of the framework.
 */
public class Run {
  private final ProjectInfo projectInfo;
  private final String scenarioName;
  private volatile int runId = -1;
  private volatile String hostname;
  private volatile String hostIp;
  private volatile Date startTime;
  private volatile Date endTime;
  private volatile Parameters parameters = Parameters.EMPTY_PARAMETERS;
  private volatile State state = State.SCHEDULED;
  private volatile String runset = Constants.DEFAULT_RUNSET;
  private volatile String clusterName = Constants.DEFAULT_CLUSTER_NAME;
  private volatile File remoteOutputPath;
  private volatile String outputHost;

  /**
    * Constructor.
    * @param projectInfo object containing info that is common to all runs in the project
    * @param scenarioName the name of the scenario which defines this run or null if this run is without scenario
    */
  public Run(ProjectInfo projectInfo, String scenarioName) {
    this.projectInfo = projectInfo;
    this.scenarioName = scenarioName;
  }

  public String getHostname() {
    return hostname;
  }

  public Run setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  public String getName() {
    return projectInfo.projectName + (scenarioName == null ? "" : "_" + scenarioName);
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

  public CodeSourceImpl getCodeSource() {
    return projectInfo.codeSource;
  }

  public String getHostIp() {
    return hostIp;
  }

  public Run setHostIp(String hostIp) {
    this.hostIp = hostIp;
    return this;
  }

  public Map<String, ? extends CodeSource> getLibraries() {
    return projectInfo.libraries;
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
    return projectInfo.projectName;
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
    return projectInfo.runnerClass;
  }

  public String getRunnerFlags() {
    return projectInfo.runnerFlags;
  }

  public State getState() {
    return state;
  }

  public Run setState(State state) {
    this.state = state;
    return this;
  }

  @Override
  public String toString() {
    return "Run{" +
        "runId=" + runId +
        ", projectInfo='" + projectInfo + '\'' +
        ", scenarioName='" + scenarioName + '\'' +
        ", state='" + state + '\'' +
        ", runset='" + runset + '\'' +
        ", clustername='" + clusterName + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Run other = (Run) obj;
    return Objects.equal(this.projectInfo, other.projectInfo) &&
        Objects.equal(this.scenarioName, other.scenarioName) &&
        Objects.equal(this.runId, other.runId) && Objects.equal(this.hostname, other.hostname) &&
        Objects.equal(this.hostIp, other.hostIp) && Objects.equal(this.startTime, other.startTime) &&
        Objects.equal(this.endTime, other.endTime) && Objects.equal(this.parameters, other.parameters) &&
        Objects.equal(this.state, other.state) && Objects.equal(this.runset, other.runset) &&
        Objects.equal(this.clusterName, other.clusterName) &&
        Objects.equal(this.remoteOutputPath, other.remoteOutputPath) &&
        Objects.equal(this.outputHost, other.outputHost);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(projectInfo, scenarioName, runId, hostname, hostIp, startTime, endTime, parameters,
        state, runset, clusterName, remoteOutputPath, outputHost);
  }

  public enum State {
    FAILED,
    COMPLETED,
    COPIED,
    COPY_FAILED,
    IN_PROGRESS,
    SCHEDULED,
  }

  public static class ProjectInfo {
    private final String projectName;
    private final CodeSourceImpl codeSource;
    private final Map<String, ? extends CodeSource> libraries;
    private final String runnerClass;
    private final String runnerFlags;

    public ProjectInfo(String projectName, CodeSourceImpl codeSource, Map<String, ? extends CodeSource> libraries,
        String runnerClass, String runnerFlags) {
      this.projectName = projectName;
      this.codeSource = codeSource;
      this.libraries = libraries;
      this.runnerClass = runnerClass;
      this.runnerFlags = runnerFlags;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(projectName, codeSource, libraries, runnerClass, runnerFlags);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ProjectInfo other = (ProjectInfo) obj;
      return Objects.equal(this.projectName, other.projectName) && Objects.equal(this.codeSource,
          other.codeSource) && Objects.equal(this.libraries, other.libraries) && Objects.equal(this.runnerClass, other.runnerClass) && Objects.equal(this.runnerFlags, other.runnerFlags);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("projectName", projectName)
          .add("codeSource", codeSource)
          .add("libraries", libraries)
          .add("runnerClass", runnerClass)
          .add("runnerFlags", runnerFlags)
          .toString();
    }
  }
}
