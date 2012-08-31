package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

import java.io.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Flags flags = Flags.parseFlags(flagsString);

    // TODO(michaell): urgh. get rid of this hard coded hackery!!
    File projectPath = new File(model, "projects/" + flags.projectName);

    File variablesFile = writeVariablesFile(outputPath, parameters);

    // TODO(michaell): put this in the jar and pipe it into R?
    // a la: cat R/rrunner.R | R --vanilla --args --paramfile=/tmp/variables.json --rscript R/example.R
    // otherwise, find somewhere else for it to live...
    File rrunnerPath = new File(model, "R/rrunner.R");

    return executeCommand(model, logger, flags.rLocation.getPath(),
        rrunnerPath.getPath(),
        "--paramfile=" + variablesFile.getPath(),
        "--rscript=" + new File(projectPath, flags.rScript.getPath()).getPath());
  }

  /**
   * Helper class to parse the flags passed to the runner. We can't use jCommander here
   * because the -p flag is historically used without a separator (eg -pexample), which
   * jCommand doesn't support. Thus, we parse the flags the old fashioned way, with
   * regular expresions.
   */
  private static class Flags {
    private static final Pattern PROJECT_FLAG = Pattern.compile(".*-p(\\S*).*");
    private static final Pattern R_LOCATION_FLAG = Pattern.compile(".*--rlocation=(\\S*).*");
    private static final Pattern R_SCRIPT_FLAG = Pattern.compile(".*--rscript=(\\S*).*");

    /**
     * Project name.
     */
    private final String projectName;

    /**
     * Path to Rscript executable.
     */
    private final File rLocation;

    /**
     * Path to R script to be executed.
     */
    private final File rScript;

    private Flags(String projectName, File rLocation, File rScript) {
      this.projectName = projectName;
      this.rLocation = rLocation;
      this.rScript = rScript;
    }

    private static Flags parseFlags(String flags) throws RdvException {
      // TODO(michaell): move project name into database so we can avoid this hackery
      // either that, or some other way to specify how to find the source code
      // If we don't do that, at least move the command line flags into the project spec.
      String projectName = parseFlag(flags, PROJECT_FLAG);
      if (projectName == null) {
        throw new RdvException("Missing -p flag for project name.");
      }
      String rlocationString = parseFlag(flags, R_LOCATION_FLAG);
      File rLocation = rlocationString == null ? new File("Rscript") : new File(rlocationString);

      String rScriptString = parseFlag(flags, R_SCRIPT_FLAG);
      if (rScriptString == null) {
        throw new RdvException("Missing --rscript flag for R script to be executed.");
      }
      File rScript = new File(rScriptString);

      return new Flags(projectName, rLocation, rScript);
    }

    private static String parseFlag(String flags, Pattern pattern) throws RdvException {
      Matcher m = pattern.matcher(flags);
      if (!m.matches()) {
        return null;
      }
      return m.group(1);
    }
  }
}
