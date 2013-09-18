package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Static utility methods for use from Runner classes.
 */
class RunnerUtils {
  protected static File writeVariablesFile(File outputPath, Parameters parameters) throws TzarException {
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
