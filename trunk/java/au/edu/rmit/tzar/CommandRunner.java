package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Runner;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the Runner interface which calls out to an executable command (run_model)_in the model code.
 * TODO(michaell): consider making this command specifiable in the database. can we do this without introducing a
 * security hole? Note that one major concern is with path traversal vulnerabilities
 * (eg file_name="../../usr/bin/do_something bad"). Recognising ".." may not be enough to catch variants of this attack
 * and its relatives.
 */
public class CommandRunner implements Runner {
  private static Logger LOG = Logger.getLogger(CommandRunner.class.getName());

  private final Joiner joiner = Joiner.on(' ');

  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger) throws TzarException {
    String[] command = new String[]{model.getPath() + "/run_model", "-v", "-o", outputPath.getAbsolutePath()};
    LOG.info("Executing: " + joiner.join(command));

    Process process;
    try {
      process = Runtime.getRuntime().exec(command, new String[]{}, model);
    } catch (IOException e) {
      throw new TzarException(e);
    }

    Utils.copyStreamToLog(process.getInputStream(), LOG, Level.INFO);
    Utils.copyStreamToLog(process.getErrorStream(), LOG, Level.WARNING);
    try {
      int retVal = process.waitFor();
      return retVal == 0;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TzarException(e);
    }
  }
}
