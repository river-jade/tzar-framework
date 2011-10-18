package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

import java.io.File;

/**
 * Runner implementation that does nothing and returns success with 50% probability.
 * (Note that this is not truly random!)
 */
public class RandomNullRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String flags, Parameters parameters)
      throws RdvException {
    return System.currentTimeMillis() % 2 == 0;
  }
}
