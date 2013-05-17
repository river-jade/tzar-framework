package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

import java.io.File;
import java.util.logging.Logger;

/**
 * Runner implementation that does nothing and returns success.
 */
public class NullRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
      Logger logger) throws RdvException {
    return true;
  }
}
