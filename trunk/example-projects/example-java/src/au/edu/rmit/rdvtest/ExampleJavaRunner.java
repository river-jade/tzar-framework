package au.edu.rmit.rdvtest;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;

import java.io.*;
import java.util.logging.Logger;

/**
 * An example java runner.
 *
 */
public class ExampleJavaRunner implements Runner {

  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    logger.info("Entering example java runner.");
    logger.info("Here are the parameters: " + parameters.toString());
    logger.info("Exiting example java runner.");
    File file = new File(outputPath, "single_result.csv");
    logger.info(file.toString());
    FileWriter writer;
    try {
      writer = new FileWriter(file);
      writer.write("value 1,value 2,value 3\n");
      writer.write(String.format("%s1,%s2,%s3\n", runId, runId, runId));
      writer.close();
    } catch (IOException e) {
      throw new TzarException(e);
    }

    BufferedReader reader;
    try {
      String pathname = parameters.asMap().get("version.library.name").toString();
      reader = new BufferedReader(new FileReader(new File(pathname)));
      logger.info("Latest tzar version is: " + reader.readLine());
    } catch (IOException e) {
      throw new TzarException(e);
    }
    return true;
  }
}
