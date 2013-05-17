package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A base class for Runners which write parameters out to a file and then call a
 * system command (eg Python, or R) and run a script.
 */
public abstract class SystemRunner {
  protected File writeVariablesFile(File outputPath, Parameters parameters) throws TzarException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    File variablesFile = new File(outputPath, "parameters.json");
    try {
      FileWriter writer = new FileWriter(variablesFile);
      gson.toJson(parameters, writer);
      writer.close();
    } catch (IOException e) {
      throw new TzarException(e);
    }
    return variablesFile;
  }

  protected boolean executeCommand(File model, Logger logger, String... command) throws TzarException {
    try {
      logger.fine(Joiner.on(" ").join(command));
      ProcessBuilder processBuilder = new ProcessBuilder(command);

      Process process = processBuilder
          .redirectErrorStream(true)
          .directory(model)
          .start();

      // Send stdout and stderr to the logger.
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        logger.fine(line);
      }
      return process.waitFor() == 0;
    } catch (IOException e) {
      throw new TzarException(e);
    } catch (InterruptedException e) {
      throw new TzarException(e);
    }
  }

  protected static <T> T parseFlags(String[] flagString, T flags) throws TzarException {
    JCommander jcommander = new JCommander(flags);
    try {
      jcommander.parse(flagString);
    } catch (ParameterException e) {
      throw new TzarException("Error parsing flag string: " + Arrays.toString(flagString), e);
    }
    return flags;
  }
}
