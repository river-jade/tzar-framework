package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.Repetitions;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.logging.Logger;

/**
 * Factory to create Run objects. Parses an (optional) global parameters file, a project spec file and an (optional)
 * repetitions file.
 */
public class RunFactory {
  private static Logger LOG = Logger.getLogger(RunFactory.class.getName());

  private final String revision;
  private final String commandFlags;
  private final String runset;
  private final String clusterName;
  private final ProjectSpec projectSpec;
  private final Repetitions repetitions;
  private final Parameters globalParams;

  public RunFactory(String revision, String commandFlags, String runset, String clusterName, ProjectSpec projectSpec,
                    Repetitions repetitions, Parameters globalParams) {
    this.revision = revision;
    this.commandFlags = commandFlags;
    this.runset = runset;
    this.clusterName = clusterName;
    this.projectSpec= projectSpec;
    this.repetitions = repetitions;
    this.globalParams = globalParams;
  }

  /**
   * Creates a list of runs.
 *
   * @param numRuns number of copies of each unique run to generate. The total number of runs generated will
   *                be this number multiplied by the number of scenarios, multiplied by the number of
   *                repetitions
   * @param runnerClass Runner implementation to use to execute the runs
   * @return the list of created runs
   * @throws RdvException
   */
  public List<Run> createRuns(int numRuns, String runnerClass) throws RdvException {
    Parameters projectParams = projectSpec.getBaseParams();
    projectParams = globalParams.mergeParameters(projectParams);
    List<Run> runs = Lists.newArrayList();
    for (int i = 0; i < numRuns; ++i) {
      runs.addAll(createRuns(projectSpec, projectParams, repetitions, runnerClass));
    }
    LOG.info("Created " + runs.size() + " runs.");
    return runs;
  }

  /**
   * Create a List of Runs, one for each repetition in the Repetitions object, for each scenario in the projectSpec.
   */
  private List<Run> createRuns(ProjectSpec projectSpec, Parameters baseParams, Repetitions repetitions,
      String runnerClass) throws RdvException {
    List<Run> runs = Lists.newArrayList();

    for (Parameters repetitionParams : repetitions.getParamsList()) {
      if (projectSpec.getScenarios() != null && projectSpec.getScenarios().size() > 0) {
        for (Scenario scenario : projectSpec.getScenarios()) {
          Parameters params = baseParams.mergeParameters(scenario.getParameters());
          params = params.mergeParameters(repetitionParams);
          runs.add(createRun(params, runnerClass, projectSpec.getProjectName(), scenario.getName()));
        }
      } else {
        runs.add(createRun(baseParams.mergeParameters(repetitionParams), runnerClass, projectSpec.getProjectName(),
            null));
      }
    }
    return runs;
  }

  private Run createRun(Parameters runParams, String runnerClass, String projectName, String scenarioName) {
    return new Run(-1, projectName, scenarioName, revision, commandFlags, runParams, "scheduled", runset, clusterName,
        runnerClass);
  }
}
