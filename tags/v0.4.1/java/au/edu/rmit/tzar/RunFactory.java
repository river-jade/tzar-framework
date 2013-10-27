package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.repository.CodeSource;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.logging.Logger;

/**
 * Factory to create Run objects. Generates runs for a given project specification.
 */
public class RunFactory {
  private static Logger LOG = Logger.getLogger(RunFactory.class.getName());

  private final CodeSource codeSource;
  private final String runset;
  private final String clusterName;
  private final ProjectSpec projectSpec;

  public RunFactory(CodeSource codeSource, String runset, String clusterName, ProjectSpec projectSpec) {
    this.codeSource = codeSource;
    this.runset = runset;
    this.clusterName = clusterName;
    this.projectSpec = projectSpec;
  }

  /**
   * Creates a list of runs.
 *
   * @param numRuns number of copies of each unique run to generate. The total number of runs generated will
   *                be this number multiplied by the number of scenarios, multiplied by the number of
   *                repetitions
   * @return the list of created runs
   * @throws TzarException
   */
  public List<Run> createRuns(int numRuns) throws TzarException {
    List<Run> runs = Lists.newArrayList();
    for (int i = 0; i < numRuns; ++i) {
      runs.addAll(createRuns());
    }
    LOG.info("Created " + runs.size() + " runs.");
    return runs;
  }

  /**
   * Create a List of Runs, one for each repetition in the Repetitions object, for each scenario in the projectSpec.
   */
  private List<Run> createRuns() throws TzarException {
    List<Run> runs = Lists.newArrayList();

    for (Parameters repetitionParams : projectSpec.getRepetitions().getParamsList()) {
      if (projectSpec.getScenarios() != null && projectSpec.getScenarios().size() > 0) {
        for (Scenario scenario : projectSpec.getScenarios()) {
          Parameters params = projectSpec.getBaseParams().mergeParameters(scenario.getParameters());
          params = params.mergeParameters(repetitionParams);
          runs.add(createRun(params, scenario.getName()));
        }
      } else {
        runs.add(createRun(projectSpec.getBaseParams().mergeParameters(repetitionParams), null));
      }
    }
    return runs;
  }

  private Run createRun(Parameters runParams, String scenarioName) {
    return new Run(projectSpec.getProjectName(), scenarioName, codeSource)
        .setRunnerFlags(projectSpec.getRunnerFlags())
        .setParameters(runParams)
        .setRunset(runset)
        .setClusterName(clusterName)
        .setRunnerClass(projectSpec.getRunnerClass());
  }
}
