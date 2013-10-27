package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.repository.CodeRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An executable Run. Instances of this class have all the information they need to execute
 * a Run.
 */
public class ExecutableRun {
  private static final Logger LOG = Logger.getLogger(ExecutableRun.class.getName());
  // this is the Logger that will be used by runner to write to the console and logfiles
  private static final Logger RUNNER_LOGGER = Logger.getLogger("au.edu.rmit.tzar.ModelRunnerLogger");

  @VisibleForTesting
  static final String INPROGRESS_SUFFIX = ".inprogress";
  @VisibleForTesting
  static final String FAILED_SUFFIX = ".failed";

  private static volatile int nextRunId = 1;

  // the output path, not including the status suffix (ie failed, inprogress etc).
  private final File baseOutputPath;
  // the output path. ie the relative path on the local machine where results will be written.
  private volatile File outputPath;

  private final Run run;
  private final CodeRepository codeRepository;
  private final RunnerFactory runnerFactory;
  private final YamlParser yamlParser = new YamlParser();

  /**
   * Factory method.
   *
   * @param run            run to execute
   * @param baseOutputPath local path for output for this run.
   * @param codeRepository the repository from which to download the model code
   * @param runnerFactory  factory to instantiate runner objects
   * @return a newly created executable run
   */
  public static ExecutableRun createExecutableRun(Run run, File baseOutputPath, CodeRepository codeRepository,
      RunnerFactory runnerFactory) throws TzarException {
    String runset = run.getRunset();
    runset = (runset == null || runset.equals("")) ? "default_runset" : run.getRunset();
    baseOutputPath = new File(baseOutputPath, Utils.Path.combineAndReplaceWhitespace("_", run.getProjectName(),
        runset));

    if (run.getRunId() == -1) {
      run.setRunId(getNextRunId(baseOutputPath));
    }
    // replace whitespace and punctuation in run name with '_' to make a valid path.
    String dirName = Utils.Path.combineAndReplaceWhitespace("_", run.getRunId() + "_" + run.getScenarioName());
    LOG.log(Level.FINER, "Creating run: {0}", run);
    return new ExecutableRun(run, new File(baseOutputPath, dirName), codeRepository, runnerFactory);
  }

  /**
   * Constructor.
   *
   * @param run            run to execute
   * @param baseOutputPath     local path for output for this run.
   * @param codeRepository the repository from which to download the model code
   * @param runnerFactory  factory to create runner instances
   */
  private ExecutableRun(Run run, File baseOutputPath, CodeRepository codeRepository, RunnerFactory runnerFactory) {
    this.run = run;
    this.codeRepository = codeRepository;
    this.runnerFactory = runnerFactory;
    this.baseOutputPath = baseOutputPath;
    this.outputPath = new File(baseOutputPath + INPROGRESS_SUFFIX);
  }

  /**
   * Execute this run.
   *
   * @return true if the run executed successfully, false otherwise
   * @throws TzarException if an error occurs executing the run
   */
  public boolean execute() throws TzarException {
    File model = codeRepository.getModel(run.getRevision());
    try {
      if (outputPath.exists()) {
        LOG.warning("Temp output path: " + outputPath + " already exists. Deleting.");
        Files.deleteRecursively(outputPath);
      }
      LOG.info("Creating temporary outputdir: " + outputPath);
      if (!outputPath.mkdirs()) {
        throw new IOException("Couldn't create temp output dir: " + outputPath);
      }

      LOG.info(String.format("Running model: %s, run_id: %d, Project name: %s, Scenario name: %s, " +
          "Flags: %s", model, getRunId(), run.getProjectName(), run.getScenarioName(), run.getRunnerFlags()));
      // TODO(michaell): Add some more wildcards here?
      Parameters parameters = run.getParameters().replaceWildcards(
          ImmutableMap.of(
              "run_id", Integer.toString(getRunId()),
              "source_root", model.getAbsolutePath(),
              "output_path", outputPath.getAbsolutePath()
          ));

      FileHandler handler = null;
      boolean success = false;
      try {
        handler = setupLogFileHandler(outputPath);
        RUNNER_LOGGER.addHandler(handler);
        File parametersFile = new File(outputPath, "parameters.yaml");
        yamlParser.parametersToYaml(parameters, parametersFile);
        Runner runner = runnerFactory.getRunner(run.getRunnerClass());
        success = runner.runModel(model, outputPath, Integer.toString(run.getRunId()), run.getRunnerFlags(),
            parameters, RUNNER_LOGGER);
      } finally {
        if (handler != null) {
          RUNNER_LOGGER.removeHandler(handler);
          handler.close();
        }
        renameOutputDir(success);
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
   * @param baseOutputPath the path to look in for prior runs
   * @return an available run id (being 1 greater than the current maximum)
   */
  private synchronized static int getNextRunId(File baseOutputPath) throws TzarException {
    int max = nextRunId;
    if (!baseOutputPath.exists()) {
      LOG.info("Outputdir doesn't exist. Creating it (and parents)");
      baseOutputPath.mkdirs();
    }
    Pattern pattern = Pattern.compile("(\\d+)(_.*)+(?:\\.failed|\\.inprogress)?");
    File[] files = baseOutputPath.listFiles();
    if (files == null)
      throw new TzarException(String.format("Output path: %s is not a directory.", baseOutputPath));
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
    File destPath = success ? baseOutputPath : new File(baseOutputPath + FAILED_SUFFIX);
    if (destPath.exists()) {
      try {
        LOG.warning("Path: " + destPath + " already exists. Deleting.");
        Files.deleteRecursively(destPath);
      } catch (IOException e) {
        throw new TzarException("Unable to delete existing path: " + destPath, e);
      }
    }
    Utils.fileRename(outputPath, destPath);
    outputPath = destPath;
  }
}
