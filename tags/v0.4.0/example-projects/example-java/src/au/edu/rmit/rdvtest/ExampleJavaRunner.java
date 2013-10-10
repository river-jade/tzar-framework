package au.edu.rmit.rdvtest;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.logging.Logger;

/**
 * An example java runner.
 *
 */
public class ExampleJavaRunner implements Runner {

  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
        Logger logger) throws TzarException {
    logger.info("Entering example java runner.");
    logger.info("Here are the parameters: " + parameters.toString());
    logger.info("Exiting example java runner.");
    return true;
  }
}
