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
   * @return the list of created runs
   * @throws RdvException
   */
  public List<Run> createRuns(int numRuns) throws RdvException {
    Parameters projectParams = projectSpec.getBaseParams();
    projectParams = globalParams.mergeParameters(projectParams);
    String projectName = projectSpec.getProjectName();
    List<Run> runs = Lists.newArrayList();
    for (int i = 0; i < numRuns; ++i) {
      runs.addAll(createRuns(projectSpec, projectParams, projectName, repetitions));
    }
    LOG.info("Created " + runs.size() + " runs.");
    return runs;
  }

  /**
   * Create a List of Runs, one for each repetition in the Repetitions object, for each scenario in the projectSpec.
   */
  private List<Run> createRuns(ProjectSpec projectSpec, Parameters baseParams, String projectName,
      Repetitions repetitions) throws RdvException {
    List<Run> runs = Lists.newArrayList();

    for (Parameters repetitionParams : repetitions.getParamsList()) {
      if (projectSpec.getScenarios() != null && projectSpec.getScenarios().size() > 0) {
        for (Scenario scenario : projectSpec.getScenarios()) {
          Parameters params = baseParams.mergeParameters(scenario.getParameters());
          params = params.mergeParameters(repetitionParams);
          runs.add(createRun(params, projectName + "_" + scenario.getName()));
        }
      } else {
        runs.add(createRun(baseParams.mergeParameters(repetitionParams), projectName));
      }
    }
    return runs;
  }

  private Run createRun(Parameters runParams, String runName) {
    return new Run(-1, runName, revision, commandFlags, runParams, "scheduled", runset, clusterName);
  }
}
