package au.edu.rmit.tzar;

import au.com.bytecode.opencsv.CSVWriter;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileWriter;
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

  public static ExecutableRun createExecutableRun(Run run, File tzarOutputPath, File tzarModelPath,
      RunnerFactory runnerFactory) throws TzarException {
    return createExecutableRun(run, tzarOutputPath, tzarModelPath, runnerFactory, false);
  }

  /**
   * Factory method.
   *
   * @param run            run to execute
   * @param tzarOutputPath base directory for all run output
   * @param tzarModelPath  local path for all tzar model code
   * @param runnerFactory  factory to instantiate runner objects
   * @return a newly created executable run
   */
  public static ExecutableRun createExecutableRun(Run run, File tzarOutputPath, File tzarModelPath,
      RunnerFactory runnerFactory, boolean dryRun) throws TzarException {

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
    if (dryRun) {
      return new DryRun(run, runOutputPath, Constants.DRY_RUN_SUFFIX, runnerFactory, tzarModelPath);
    } else {
      return new ExecutableRun(run, runOutputPath, Constants.INPROGRESS_SUFFIX, runnerFactory, tzarModelPath);
    }
  }

  /**
   * Constructor.
   *
   * @param run           run to execute
   * @param runOutputPath local path for output for this run
   * @param runnerFactory factory to create runner instances
   * @param baseModelPath base path for the model code to be downloaded to
   */
  protected ExecutableRun(Run run, File runOutputPath, String initialSuffix, RunnerFactory runnerFactory,
      File baseModelPath) {
    this.run = run;
    this.runnerFactory = runnerFactory;
    this.runOutputPath = runOutputPath;
    this.baseModelPath = baseModelPath;
    this.outputPath = new File(runOutputPath + initialSuffix);
  }

  /**
   * Execute this run. Checked exceptions are caught, and result in a failed run.
   *
   * @return true if the run executed successfully, false otherwise
   */
  public boolean execute() {
    return execute(new StopRun());
  }

  /**
   * Execute this run. Checked exceptions are caught, and result in a failed run.
   *
   * @param stopRun construct for stopping mid run, if this is supported by the runner
   * @return true if the run executed successfully, false otherwise
   */
  public boolean execute(StopRun stopRun) {
    try {
      CodeSource codeSource = run.getCodeSource();
      File model = codeSource.getCode(baseModelPath, run.getProjectName());
      try {
        if (outputPath.exists()) {
          LOG.warning("Local output path: " + outputPath + " already exists. Deleting.");
          deleteRecursively(outputPath);
        }
        LOG.fine("Creating output directory");
        LOG.info("Outputdir: " + outputPath);

        if (!outputPath.mkdirs()) {
          throw new IOException("Couldn't create local output dir: " + outputPath);
        }
        File metadataPath = new File(outputPath, Constants.METADATA_DIRECTORY_NAME);
        if (!metadataPath.mkdir()) {
          throw new IOException("Couldn't create local metadata dir: " + metadataPath);
        }

        LOG.info(String.format("Running model: %s, run_id: %d, Project name: %s, Scenario name: %s, " +
                "Flags: %s", model, getRunId(), run.getProjectName(), run.getScenarioName(), run.getRunnerFlags()));

        // Load the libraries
        ImmutableMap<String, File> libraries = loadLibraries();
        WildcardReplacer.Context context = new WildcardReplacer.Context(getRunId(), model, libraries, outputPath,
            metadataPath, getRun().getRunset());
        Parameters parameters = new WildcardReplacer().replaceWildcards(run.getParameters(), context);

        FileHandler handler = setupLogFileHandler(metadataPath);
        RUNNER_LOGGER.addHandler(handler);
        RUNNER_LOGGER.log(Level.FINE, DATE_FORMAT.format(new Date()));
        RUNNER_LOGGER.log(Level.FINE, "Executing run with revision: {0}, from project: {1}",
            new Object[]{defaultIfEmpty(codeSource.getRevision(), "none"), codeSource.getSourceUri()});

        writeLibraryMetadata(metadataPath);

        File parametersFile = new File(metadataPath, "parameters.yaml");
        yamlParser.parametersToYaml(parameters, parametersFile);

        return runModel(stopRun, model, parameters, handler);
      } catch (IOException e) {
        throw new TzarException(e);
      }
    } catch (TzarException e) {
      LOG.log(Level.SEVERE, "An exception occurred executing the run.", e);
      return false;
    }
  }

  /**
   * This is factored out so that we can override it in DryRun.
   */
  protected boolean runModel(StopRun stopRun, File model, Parameters parameters,
      FileHandler handler) throws TzarException {
    boolean success = false;
    try {
      Runner runner = runnerFactory.getRunner(run.getRunnerClass());
      success = runner.runModel(model, outputPath, Integer.toString(run.getRunId()), run.getRunnerFlags(),
          parameters, RUNNER_LOGGER, stopRun);
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
  }

  /**
   * Writes information about the libraries used in this run into the output directory.
   * @param metadataPath file to the directory in which to write the metadata
   * @throws TzarException
   */
  private void writeLibraryMetadata(File metadataPath) throws TzarException {
    Map<String, ? extends CodeSource> libraries = run.getLibraries();
    File libraryMetadata = new File(metadataPath, "libraries.csv");
    try {
      CSVWriter csvWriter = new CSVWriter(new FileWriter(libraryMetadata));
      try {
        csvWriter.writeNext(new String[]{"library_name", "repository_type", "uri", "revision", "download_mode"});
        for (Map.Entry<String, ? extends CodeSource> entry : libraries.entrySet()) {
          CodeSource codeSource = entry.getValue();
          csvWriter.writeNext(new String[]{entry.getKey(), codeSource.getRepositoryType().toString(),
              codeSource.getSourceUri().toString(), codeSource.getRevision(),
              codeSource.getDownloadMode().toString()});
        }
      } finally {
        csvWriter.close();
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  /**
   * Loads user libraries, and returns a map of library names to paths containing the downloaded
   * library code.
   *
   * @return
   * @throws TzarException
   */
  protected ImmutableMap<String, File> loadLibraries() throws TzarException {
    ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
    for (Map.Entry<String, ? extends CodeSource> entry : run.getLibraries().entrySet()) {
      String libraryName = entry.getKey();
      builder.put(libraryName, entry.getValue().getCode(baseModelPath, libraryName));
    }
    return builder.build();
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
      LOG.fine("Outputdir doesn't exist. Creating it (and parents)");
      runsetOutputPath.mkdirs();
    }
    Pattern pattern = Pattern.compile("(\\d+)(_.*)+(?:\\.failed|\\.inprogress)?");
    File[] files = runsetOutputPath.listFiles();
    if (files == null)
      throw new TzarException(String.format("Output path: %s is not a directory.", runsetOutputPath));
    for (File file : files) {
      if (!file.isDirectory()) {
        continue;
      }
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
