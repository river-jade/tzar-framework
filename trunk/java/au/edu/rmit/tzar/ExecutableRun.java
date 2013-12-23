package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static au.edu.rmit.tzar.Utils.*;

/**
 * An executable Run. Instances of this class have all the information they need to execute
 * a Run.
 */
public class ExecutableRun {
  private static final Logger LOG = Logger.getLogger(ExecutableRun.class.getName());
  // this is the Logger that will be used by runner to write to the console and logfiles
  private static final Logger RUNNER_LOGGER = Logger.getLogger("au.edu.rmit.tzar.ModelRunnerLogger");

  public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

  private static volatile int nextRunId = 1;
  // the output path, not including the status suffix (ie failed, inprogress etc).
  private final File runOutputPath;
  private final File baseModelPath;

  // the output path. ie the relative path on the local machine where results will be written.
  private volatile File outputPath;
  private final Run run;
  private final RunnerFactory runnerFactory;
  private final YamlParser yamlParser = new YamlParser();

  /**
   * Factory method.
   *
   * @param run            run to execute
   * @param tzarOutputPath base directory for all run output
   * @param tzarModelPath local path for all tzar model code
   * @param runnerFactory  factory to instantiate runner objects
   * @return a newly created executable run
   */
  public static ExecutableRun createExecutableRun(Run run, File tzarOutputPath, File tzarModelPath,
      RunnerFactory runnerFactory) throws TzarException {

    File runsetOutputPath = Utils.createRunsetOutputPath(tzarOutputPath, run.getProjectName(),
        run.getRunset());

    // TODO(river): this is a bit dodgy. if some bug caused the run id not to be set, then we would give
    // the run a bogus id, and potentially overwrite an existing run in the db. not quite sure how to resolve it
    // though.
    if (run.getRunId() == -1) {
      run.setRunId(getNextRunId(runsetOutputPath));
    }

    String runDirectoryName = run.getRunDirectoryName();
    LOG.log(Level.FINER, "Creating run: {0}", run);
    File runOutputPath = new File(runsetOutputPath, runDirectoryName);
    return new ExecutableRun(run, runOutputPath, runnerFactory, tzarModelPath);
  }

  /**
   * Constructor.
   *
   * @param run            run to execute
   * @param runOutputPath  local path for output for this run
   * @param runnerFactory  factory to create runner instances
   * @param baseModelPath
   */
  private ExecutableRun(Run run, File runOutputPath, RunnerFactory runnerFactory, File baseModelPath) {
    this.run = run;
    this.runnerFactory = runnerFactory;
    this.runOutputPath = runOutputPath;
    this.baseModelPath = baseModelPath;
    this.outputPath = new File(runOutputPath + Constants.INPROGRESS_SUFFIX);
  }

  /**
   * Execute this run.
   *
   * @return true if the run executed successfully, false otherwise
   * @throws TzarException if an error occurs executing the run
   */
  public boolean execute() throws TzarException {
    CodeSource codeSource = run.getCodeSource();
    File model = codeSource.getCode(baseModelPath, run.getProjectName());
    try {
      if (outputPath.exists()) {
        LOG.warning("Temp output path: " + outputPath + " already exists. Deleting.");
        deleteRecursively(outputPath);
      }
      LOG.info("Creating temporary outputdir: " + outputPath);
      if (!outputPath.mkdirs()) {
        throw new IOException("Couldn't create temp output dir: " + outputPath);
      }

      LOG.info(String.format("Running model: %s, run_id: %d, Project name: %s, Scenario name: %s, " +
          "Flags: %s", model, getRunId(), run.getProjectName(), run.getScenarioName(), run.getRunnerFlags()));

      ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
          .put("run_id", Integer.toString(getRunId()))
          .put("model_path", model.getAbsolutePath() + File.separator)
          .put("output_path", outputPath.getAbsolutePath() + File.separator);
      for (Map.Entry<String, ? extends CodeSource> entry : run.getLibraries().entrySet()) {
        String libraryName = entry.getKey();
        builder.put(String.format("library_path(%s)", libraryName),
            entry.getValue().getCode(baseModelPath, libraryName).toString());
      }

      Map<String, String> wildcards = builder.build();
      Parameters parameters = run.getParameters().replaceWildcards(wildcards);

      FileHandler handler = setupLogFileHandler(outputPath);
      RUNNER_LOGGER.addHandler(handler);
      RUNNER_LOGGER.log(Level.INFO, DATE_FORMAT.format(new Date()));
      RUNNER_LOGGER.log(Level.INFO, "Executing run with revision: {0}, from project: {1}",
          new Object[]{defaultIfEmpty(codeSource.getRevision(), "none"), codeSource.getSourceUri()});
      File parametersFile = new File(outputPath, "parameters.yaml");

      boolean success = false;
      try {
        yamlParser.parametersToYaml(parameters, parametersFile);
        Runner runner = runnerFactory.getRunner(run.getRunnerClass());
        success = runner.runModel(model, outputPath, Integer.toString(run.getRunId()), run.getRunnerFlags(),
            parameters, RUNNER_LOGGER);
      } finally {
        RUNNER_LOGGER.removeHandler(handler);
        handler.close();
        renameOutputDir(success);
      }
      if (success) {
        LOG.info("Run " + getRunId() + " succeeded.");
      } else {
        LOG.warning("Run " + getRunId() + " failed.");
      }

      return success;
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  /**
   * Calculates the next available run id from the output path. This is only used for local
   * runs. For database runs, the id must be globally unique and is provided by the database.
   *
   * @param runsetOutputPath the path to look in for prior runs
   * @return an available run id (being 1 greater than the current maximum)
   */
  private synchronized static int getNextRunId(File runsetOutputPath) throws TzarException {
    int max = nextRunId;
    if (!runsetOutputPath.exists()) {
      LOG.info("Outputdir doesn't exist. Creating it (and parents)");
      runsetOutputPath.mkdirs();
    }
    Pattern pattern = Pattern.compile("(\\d+)(_.*)+(?:\\.failed|\\.inprogress)?");
    File[] files = runsetOutputPath.listFiles();
    if (files == null)
      throw new TzarException(String.format("Output path: %s is not a directory.", runsetOutputPath));
    for (File file : files) {
      Matcher matcher = pattern.matcher(file.getName());
      if (matcher.matches() && matcher.groupCount() >= 2) {
        try {
          int id = Integer.parseInt(matcher.group(1));
          max = Math.max(max, id + 1);
        } catch (NumberFormatException e) {
          LOG.fine(file + " does not match expected pattern for output directory.");
        }
      } else {
        LOG.fine(file + " does not match expected pattern for output directory.");
      }
    }

    // we store this in a static variable to avoid the race condition where two threads get the same id.
    nextRunId = max;
    return nextRunId;
  }

  private static FileHandler setupLogFileHandler(File outputPath) throws IOException {
    FileHandler handler = new FileHandler(new File(outputPath, "logging.log").getPath());
    handler.setFormatter(new BriefLogFormatter());
    return handler;
  }

  /**
   * Gets the local file path where output from this run will be written once it is complete.
   *
   * @return the file path
   */
  public File getOutputPath() {
    return outputPath;
  }

  public Run getRun() {
    return run;
  }

  public int getRunId() {
    return run.getRunId();
  }

  private void renameOutputDir(boolean success) throws TzarException {
    File destPath = success ? runOutputPath : new File(runOutputPath + Constants.FAILED_SUFFIX);
    if (destPath.exists()) {
      try {
        LOG.warning("Path: " + destPath + " already exists. Deleting.");
        deleteRecursively(destPath);
      } catch (IOException e) {
        throw new TzarException("Unable to delete existing path: " + destPath, e);
      }
    }
    fileRename(outputPath, destPath);
    outputPath = destPath;
  }
}
