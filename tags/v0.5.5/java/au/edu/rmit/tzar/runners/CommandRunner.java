package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.logging.Logger;

/**
 * An implementation of the Runner interface which calls out to an executable command (run_model) in the model code.
 */
public class CommandRunner extends SystemRunner {
  private static Logger LOG = Logger.getLogger(CommandRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    String[] command = new String[]{model.getPath() + "/run_model", "-v", "-o", outputPath.getAbsolutePath()};
    return executeCommand(model, logger, Maps.<String, String>newHashMap(), stopRun, command);
  }
}
