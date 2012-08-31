package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.*;
import java.util.logging.Logger;

/**
 * Runner implementation to run Python code. This Runner works by writing the parameters
 * for the run out as a json file containing key-value pairs (input and output file variables
 * are qualified by the input and output paths respectively), executing
 * a wrapper script written in python, which can parse the variables.json file.
 */
public class PythonRunner extends SystemRunner implements Runner {
  private static Logger LOG = Logger.getLogger(PythonRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
                          Logger logger) throws RdvException {
    Flags flags = Flags.parseFlags(flagsString.split(" "));

    File variablesFile = writeVariablesFile(outputPath, parameters);

    try {
      File pythonRunner = File.createTempFile("pythonrunner", ".py");
      File baseModel = new File(pythonRunner.getParentFile(), "basemodel.py");
      extractResource("python/pythonrunner.py", pythonRunner);
      extractResource("python/basemodel.py", baseModel);

      return executeCommand(model, logger, flags.pythonLocation.getPath(),
          pythonRunner.getPath(),
          "--paramfile=" + variablesFile.getPath(),
          "--modelpath=" + model,
          "--outputpath=" + outputPath,
          "--runid=" + runId,
          "-p" + flags.projectName);
    } catch (IOException e) {
      throw new RdvException("Couldn't create temporary file");
    }
  }

  private File extractResource(String resourceName, File outputFile) throws RdvException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        getClass().getClassLoader().getResourceAsStream(resourceName))); // TODO(river): check for null

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
      throw new RdvException("Couldn't read pythonrunner.py");
    }
    return outputFile;
  }

  /**
   * Helper class to parse the flags passed to the runner. We can't use jCommander here
   * because the -p flag is historically used without a separator (eg -pexample), which
   * jCommand doesn't support. Thus, we parse the flags the old fashioned way, with
   * regular expressions.
   */

  @com.beust.jcommander.Parameters(separators = "= ")
  private static class Flags {
    /**
     * Project name.
     */
    @Parameter(names = "-p", description = "Name of the project to run.", required = true)
    private final String projectName = null;

    /**
     * Path to Rscript executable.
     */
    @Parameter(names = "--python-location", description = "Name of the python executable. Default: python")
    private final File pythonLocation = new File("python");

    private static Flags parseFlags(String[] flagString) throws RdvException {
      Flags flags = new Flags();
      JCommander jcommander = new JCommander(flags);
      jcommander.parse(flagString);
      return flags;
    }
  }
}
