package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Runner implementation that does nothing and returns success with 50% probability.
 */
public class RandomNullRunner implements Runner {
  private final Random random = new Random();

  @Override
  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
      Logger logger) throws TzarException {
    boolean success = random.nextBoolean();
    logger.info("Run " + (success ? "succeeded" : "failed"));
    return success;
  }
}
