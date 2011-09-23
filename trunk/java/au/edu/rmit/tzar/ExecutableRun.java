package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.repository.CodeRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * An executable Run. Instances of this class have all the information they need to execute
 * a Run.
 */
public class ExecutableRun {
  private static final Logger LOG = Logger.getLogger(ExecutableRun.class.getName());

  private static final String INPROGRESS_SUFFIX = ".inprogress";

  private static int nextRunId = 1;

  private final File tmpOutputPath;
  private final File localOutputPath;
  private final Run run;
  private final CodeRepository codeRepository;
  private final Runner runner;

  /**
   * Factory method.
   *
   * @param run            run to execute
   * @param baseOutputPath local path for output for this run.
   * @param codeRepository the repository from which to download the model code
   * @param runner         the runner to use to execute this run
   * @return a newly created executable run
   */
  public static ExecutableRun createExecutableRun(Run run, File baseOutputPath, CodeRepository codeRepository,
      Runner runner) {
    if (run.getRunId() == -1) {
      run.setRunId(getNextRunId(baseOutputPath));
    }
    // replace whitespace and punctuation in run name with '_' to make a valid path.
    String dirName = run.getName().replaceAll("\\W", "_") + "_" + run.getRunId();
    LOG.finer(String.format("Creating run: %s", run));
    return new ExecutableRun(run, new File(baseOutputPath, dirName), codeRepository, runner);
  }

  /**
   * Constructor.
   *
   * @param run            run to execute
   * @param outputPath     local path for output for this run.
   * @param codeRepository the repository from which to download the model code
   * @param runner         the runner to use to execute this run
   */
  private ExecutableRun(Run run, File outputPath, CodeRepository codeRepository, Runner runner) {
    this.run = run;
    this.codeRepository = codeRepository;
    this.runner = runner;
    tmpOutputPath = new File(outputPath + INPROGRESS_SUFFIX);
    localOutputPath = outputPath;
  }

  /**
   * Execute this run.
   *
   * @return true if the run executed successfully, false otherwise
   * @throws RdvException if an error occurs executing the run
   */
  public boolean execute() throws RdvException {
    // TODO(michaell): write unit tests for this method.
    File model = codeRepository.getModel(run.getRevision());

    try {
      if (tmpOutputPath.exists()) {
        LOG.warning("Temp output path: " + tmpOutputPath + " already exists. Deleting.");
        Files.deleteRecursively(tmpOutputPath);
      }
      LOG.fine("Creating temp dir:" + tmpOutputPath);
      if (!tmpOutputPath.mkdirs()) {
        throw new IOException("Couldn't create temp output dir: " + tmpOutputPath);
      }

      LOG.info(String.format("Running model: %s, run_id: %d, Run name: %s, Flags: %s", model, getRunId(),
          run.getName(), run.getFlags()));
      // TODO(michaell): Add some more wildcards here?
      Parameters parameters = run.getParameters().replaceWildcards(ImmutableMap.of("run_id",
          Integer.toString(getRunId())));

      FileHandler handler = null;
      boolean success = false;
      try {
        handler = setupFileLogger(tmpOutputPath);
        success = runner.runModel(model, tmpOutputPath, Integer.toString(run.getRunId()), run.getFlags(),
            parameters);
      } finally {
        if (handler != null) {
          closeFileLogger(handler);
        }
      }
      if (success) {
        renameOutputDir();
        run.setOutputPath(localOutputPath);
      }
      return success;
    } catch (IOException e) {
      throw new RdvException(e);
    }
  }

  /**
   * Calculates the next available run id from the output path. This is only used for local
   * runs. For database runs, the id must be globally unique and is provided by the database.
   *
   * @param baseOutputPath the path to look in for prior runs
   * @return an available run id (being 1 greater than the current maximum)
   */
  private synchronized static int getNextRunId(File baseOutputPath) {
    int max = nextRunId;
    if (!baseOutputPath.exists()) {
      LOG.info("Outputdir doesn't exist. Creating it (and parents)");
      baseOutputPath.mkdirs();
    }
    for (File file : baseOutputPath.listFiles()) {
      String[] parts = file.getName().split("_");
      if (parts.length > 1) {
        try {
          String lastPart = parts[parts.length - 1];
          if (lastPart.endsWith(INPROGRESS_SUFFIX)) {
            lastPart = lastPart.substring(0, lastPart.length() - INPROGRESS_SUFFIX.length());
          }
          int id = Integer.parseInt(lastPart);
          max = Math.max(max, id + 1);
        } catch (NumberFormatException e) {
          // don't need to do anything here. value was not a number, so just continue.
        }
      }
    }

    // we store this in a static variable to avoid the race condition where two threads get the same id.
    nextRunId = max;
    return nextRunId;
  }

  private static FileHandler setupFileLogger(File outputPath) throws IOException {
    FileHandler handler = new FileHandler(new File(outputPath, "logging.log").getPath());
    Logger.getLogger("").addHandler(handler);
    return handler;
  }

  private static void closeFileLogger(FileHandler handler) throws IOException {
    handler.close();
    Logger.getLogger("").removeHandler(handler);
  }

  /**
   * Gets the local file path where output from this run will be written once it is complete.
   *
   * @return the file path
   */
  public File getLocalOutputPath() {
    return localOutputPath;
  }

  public Run getRun() {
    return run;
  }

  public int getRunId() {
    return run.getRunId();
  }

  private void renameOutputDir() throws RdvException {
    if (localOutputPath.exists()) {
      try {
        LOG.warning("Output path: " + localOutputPath + " already exists. Deleting.");
        Files.deleteRecursively(localOutputPath);
      } catch (IOException e) {
        throw new RdvException("Unable to delete existing output path: " + localOutputPath, e);
      }
    }
    Utils.fileRename(tmpOutputPath, localOutputPath);
  }
}
