package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.Repetitions;
import au.edu.rmit.tzar.parser.YamlParser;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Factory to create Run objects. Parses an (optional) global parameters file, a project spec file and an (optional)
 * repetitions file.
 */
public class RunFactory {
  private static Logger LOG = Logger.getLogger(RunFactory.class.getName());

  private final YamlParser yamlParser;
  private final String revision;
  private final String commandFlags;
  private final String runset;
  private final File projectSpecPath;
  private final File repetitionsPath;
  private final File globalParamsPath;

  public RunFactory(YamlParser yamlParser, String revision, String commandFlags, String runset, File projectSpecPath,
      File repetitionsPath, File globalParamsPath) {
    this.yamlParser = yamlParser;
    this.revision = revision;
    this.commandFlags = commandFlags;
    this.runset = runset;
    this.projectSpecPath = projectSpecPath;
    this.repetitionsPath = repetitionsPath;
    this.globalParamsPath = globalParamsPath;
  }

  /**
   * @param numRuns
   * @return
   * @throws RdvException
   */
  public List<Run> createRuns(int numRuns) throws RdvException {
    ProjectSpec projectSpec = getProjectSpec();
    Parameters projectParams = projectSpec.getBaseParams();
    Parameters globalParams = getGlobalParams();
    projectParams = globalParams.mergeParameters(projectParams);
    Repetitions repetitions = getRepetitions();

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
  private List<Run> createRuns(ProjectSpec projectSpec, Parameters params, String projectName,
      Repetitions repetitions) throws RdvException {
    List<Run> runs = Lists.newArrayList();
    for (Parameters repetitionParams : repetitions.getParamsList()) {
      runs.addAll(createRuns(projectSpec, params.mergeParameters(repetitionParams), projectName));
    }
    return runs;
  }

  /**
   * Create a list of runs given the provided parameters and the Repetitions object.
   * One run will be created for every repetition.
   */
  private List<Run> createRuns(ProjectSpec projectSpec, Parameters params, String projectName) throws RdvException {
    List<Run> runs = Lists.newArrayList();
    if (projectSpec.getScenarios() != null && projectSpec.getScenarios().size() > 0) {
      for (Scenario scenario : projectSpec.getScenarios()) {
        Parameters runParams = params.mergeParameters(scenario.getParameters());
        runs.add(createRun(runParams, projectName + "_" + scenario.getName()));
      }
    } else {
      runs.add(createRun(params, projectName));
    }
    return runs;
  }

  private Run createRun(Parameters runParams, String runName) {
    return new Run(-1, runName, revision, commandFlags, runParams, "scheduled", runset);
  }

  private Parameters getGlobalParams() throws RdvException {
    try {
      if (globalParamsPath != null) {
        return yamlParser.parametersFromYaml(globalParamsPath);
      } else {
        return Parameters.EMPTY_PARAMETERS;
      }
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }

  private ProjectSpec getProjectSpec() throws RdvException {
    try {
      ProjectSpec projectSpec = yamlParser.projectSpecFromYaml(projectSpecPath);
      projectSpec.validate();
      return projectSpec;
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }

  private Repetitions getRepetitions() throws RdvException {
    try {
      if (repetitionsPath != null) {
        return yamlParser.repetitionsFromYaml(repetitionsPath);
      } else {
        return Repetitions.EMPTY_REPETITIONS;
      }
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }
}
