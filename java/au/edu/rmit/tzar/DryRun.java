package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.runners.RunnerFactory;

import java.io.File;
import java.util.logging.FileHandler;

/**
 * Created by michaell on 11/12/14.
 */
public class DryRun extends ExecutableRun {
  /**
   * Constructor.
   *
   * @param run           run to execute
   * @param runOutputPath local path for output for this run
   * @param initialSuffix
   * @param runnerFactory factory to create runner instances
   * @param baseModelPath base path for the model code to be downloaded to
   */
  protected DryRun(Run run, File runOutputPath, String initialSuffix, RunnerFactory runnerFactory, File baseModelPath) {
    super(run, runOutputPath, initialSuffix, runnerFactory, baseModelPath);
  }

  @Override
  protected boolean runModel(StopRun stopRun, File model, Parameters parameters, FileHandler handler) throws TzarException {
    return true; // dry runs always succeed.
  }
}
