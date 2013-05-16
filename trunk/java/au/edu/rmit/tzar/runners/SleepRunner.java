package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.logging.Logger;

/**
 * Runner that sleeps for 5 seconds then succeeds.
 */
public class SleepRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
      Logger logger) throws TzarException {
    try {
      Thread.sleep(5000);
      return true;
    } catch (InterruptedException e) {
      throw new TzarException(e);
    }
  }
}
