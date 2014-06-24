package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Runner implementation to run R code. This Runner works by writing the parameters
 * for the run out as a json file containing key-value pairs (input and output file variables
 * are qualified by the input and output paths respectively), executing
 * a wrapper script written in R, which can parse the variables.json file.
 */
public class RRunner extends SystemRunner implements Runner {
  private static Logger LOG = Logger.getLogger(RRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    Flags flags = RunnerUtils.parseFlags(runnerFlags.split(" "), new Flags());

    File variablesFile = RunnerUtils.writeTempVariablesFile(parameters);

    File rRunnerPath;
    try {
      rRunnerPath = File.createTempFile("rrunner", ".R");
    } catch (IOException e) {
      throw new TzarException(e);
    }
    RunnerUtils.extractResource("R/rrunner.R", rRunnerPath);

    return executeCommand(model, logger, ImmutableMap.<String, String>of(), stopRun, flags.rLocation.getPath(),
        rRunnerPath.toString(),
        "--paramfile=" + variablesFile.getPath(),
        "--rscript=" + new File(model, flags.rScript.getPath()).getPath(),
        "--outputpath=" + outputPath.getAbsolutePath() + File.separator,
        "--inputpath=" + model
    );
  }

  /**
   * Helper class to parse the flags passed to the runner.
   */
  @com.beust.jcommander.Parameters(separators = "= ")
  private static class Flags {
    /**
     * Path to Rscript executable.
     */
    @Parameter(names = "--rlocation", description = "Name of the R executable. Default: Rscript")
    private final File rLocation = new File("Rscript");

    /**
     * Path to R script to be executed.
     */
    @Parameter(names = "--rscript", description = "Name of the R script to execute. Default: model.R")
    private File rScript = new File("model.R");
  }
}
