package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Static utility methods for use from Runner classes.
 */
class RunnerUtils {
  static File writeTempVariablesFile(Parameters parameters) throws TzarException {
    try {
      File outputFile = File.createTempFile("parameters", ".json");
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      FileWriter writer = new FileWriter(outputFile);
      gson.toJson(parameters, writer);
      writer.close();
      return outputFile;
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  static <T> T parseFlags(String[] flagString, T flags) throws TzarException {
    JCommander jcommander = new JCommander(flags);
    try {
      jcommander.parse(flagString);
    } catch (ParameterException e) {
      throw new TzarException("Error parsing flag string: " + Arrays.toString(flagString), e);
    }
    return flags;
  }

  static File extractResource(String resourceName, File outputFile) throws TzarException {
    InputStream resourceStream = RunnerUtils.class.getClassLoader().getResourceAsStream(resourceName);
    if (resourceStream == null) {
      throw new TzarException("Couldn't find required resource: " + resourceName);
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));

    try {
      FileOutputStream outputStream = new FileOutputStream(outputFile);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
      String line;
      while((line = reader.readLine()) != null) {
        writer.write(line);
        writer.newLine();
      }
      writer.close();
    } catch (IOException e) {
      throw new TzarException(String.format("Couldn't extract resource %s.", resourceName), e);
    }
    return outputFile;
  }

  static File extractResourceToFile(File directory, final String packageName, String filename) throws TzarException {
    File resourceFile = new File(directory, filename);
    extractResource(packageName + filename, resourceFile);
    return resourceFile;
  }
}
