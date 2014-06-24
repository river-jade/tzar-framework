package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.logging.Logger;

/**
 * Runner implementation that does nothing and returns success.
 */
public class NullRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    return true;
  }
}
