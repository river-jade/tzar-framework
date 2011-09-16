package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Factory to create Run objects. Parses an (optional) global parameters file, a project spec file and an (optional)
 * repetitions file.
 */
public class RunFactory {
  private final JsonParser jsonParser;
  private final String revision;
  private final String commandFlags;
  private final String runset;
  private final File projectSpecPath;
  private final File repetitionsPath;
  private final File globalParamsPath;
  private volatile int baseRunId;

  public RunFactory(JsonParser jsonParser, String revision, String commandFlags, String runset, File projectSpecPath,
      File repetitionsPath, File globalParamsPath, int baseRunId) {
    this.jsonParser = jsonParser;
    this.revision = revision;
    this.commandFlags = commandFlags;
    this.runset = runset;
    this.projectSpecPath = projectSpecPath;
    this.repetitionsPath = repetitionsPath;
    this.globalParamsPath = globalParamsPath;
    this.baseRunId = baseRunId;
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
    return new Run(baseRunId++, runName, revision, commandFlags, runParams, "scheduled", runset);
  }

  private Parameters getGlobalParams() throws RdvException {
    try {
      if (globalParamsPath != null) {
        return jsonParser.parametersFromJson(globalParamsPath);
      } else {
        return Parameters.EMPTY_PARAMETERS;
      }
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }

  private ProjectSpec getProjectSpec() throws RdvException {
    try {
      ProjectSpec projectSpec = jsonParser.projectSpecFromJson(projectSpecPath);
      projectSpec.validate();
      return projectSpec;
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }

  private Repetitions getRepetitions() throws RdvException {
    try {
      if (repetitionsPath != null) {
        return jsonParser.repetitionsFromJson(repetitionsPath);
      } else {
        return Repetitions.EMPTY_REPETITIONS;
      }
    } catch (FileNotFoundException e) {
      throw new RdvException(e);
    }
  }
}