package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Runner implementation to run Python code. This Runner works by writing the parameters
 * for the run out as a json file containing key-value pairs (input and output file variables
 * are qualified by the input and output paths respectively), executing
 * a wrapper script written in python, which can parse the variables.json file.
 */
public class PythonRunner extends SystemRunner implements Runner {
  private static Logger LOG = Logger.getLogger(PythonRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
                          Logger logger) throws TzarException {
    Flags flags = RunnerUtils.parseFlags(runnerFlags.split(" "), new Flags());

    File variablesFile = RunnerUtils.writeTempVariablesFile(parameters);

    File tempDirectory = Files.createTempDir();
    File pythonRunner = RunnerUtils.extractResourceToFile(tempDirectory, "python/", "pythonrunner.py");
    RunnerUtils.extractResourceToFile(tempDirectory, "python/", "basemodel.py");

    Map<String, String> env = ImmutableMap.of("PYTHONPATH", model.getAbsolutePath());
    return executeCommand(model, logger, env, flags.pythonLocation.getPath(),
        pythonRunner.getPath(),
        "--paramfile=" + variablesFile.getPath(),
        "--modelpath=" + model,
        "--outputpath=" + outputPath,
        "--runid=" + runId);
  }

  @com.beust.jcommander.Parameters(separators = "= ")
  private static class Flags {
    /**
     * Path to python executable.
     */
    @Parameter(names = "--python-location", description = "Name of the python executable. Default: python")
    private final File pythonLocation = new File("python");
  }
}
