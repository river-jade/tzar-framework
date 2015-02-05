package tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.logging.Logger;

/**
 * Example Java runner. This one is in the root level package, and has the default name,
 * so you don't need to pass a --classname flag to execute it.
 */
public class RunnerImpl implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    logger.info("Entering RunnerImpl runner.");
    logger.info("Here are the parameters: " + parameters.toString());
    logger.info("Exiting RunnerImpl runner.");
    return true;

  }
}
