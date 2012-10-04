package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.logging.Logger;

/**
 * Runner implementation to run R code. This Runner works by writing the parameters
 * for the run out as a json file containing key-value pairs (input and output file variables
 * are qualified by the input and output paths respectively), executing
 * a wrapper script written in R, which can parse the variables.json file.
 */
public class RRunner  extends SystemRunner implements Runner {
  private static Logger LOG = Logger.getLogger(RRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String flagsString, Parameters parameters,
      Logger logger) throws RdvException {
    Flags flags = parseFlags(flagsString.split(" "), new Flags());

    // TODO(michaell): urgh. get rid of this hard coded hackery!!
    String projectPath = Utils.Path.combine(model.getPath(), "projects/" + flags.projectName) + File.separator;

    File variablesFile = writeVariablesFile(outputPath, parameters);

    // TODO(michaell): put this in the jar and pipe it into R?
    // a la: cat R/rrunner.R | R --vanilla --args --paramfile=/tmp/variables.json --rscript R/example.R
    // otherwise, find somewhere else for it to live...
    String rrunnerPath = Utils.Path.combine(model.getPath(), "R/rrunner.R");

    return executeCommand(model, logger, flags.rLocation.getPath(),
        rrunnerPath,
        "--paramfile=" + variablesFile.getPath(),
        "--rscript=" + new File(projectPath, flags.rScript.getPath()).getPath(),
        "--outputpath=" + outputPath.getAbsolutePath() + File.separator,
        "--inputpath=" + projectPath
    );
  }

  /**
   * Helper class to parse the flags passed to the runner.
   */
  @com.beust.jcommander.Parameters(separators = "= ")
  private static class Flags {
    /**
     * Project name.
     */
    @Parameter(names = "-p", description = "Name of the project to run.", required = true)
    private String projectName;

    /**
     * Path to Rscript executable.
     */
    @Parameter(names = "--rlocation", description = "Name of the R executable. Default: Rscript")
    private final File rLocation = new File("Rscript");

    /**
     * Path to R script to be executed.
     */
    @Parameter(names = "--rscript", description = "Name of the R script to execute.", required = true)
    private File rScript;
  }
}
