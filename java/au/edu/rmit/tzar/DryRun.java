package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.Map;
import java.util.logging.FileHandler;

/**
 * An ExecutableRun that doesn't download the libraries, run the mapreduce, or
 * actually execute the model
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

  /**
   * Do nothing library loader.
   * @return
   * @throws TzarException
   */
  @Override
  protected ImmutableMap<String, File> loadLibraries() throws TzarException {
    ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
    for (Map.Entry<String, ? extends CodeSource> entry : getRun().getLibraries().entrySet()) {
      String libraryName = entry.getKey();
      builder.put(libraryName, new File("/dummy/library/path/" + libraryName));
    }
    return builder.build();
  }
}
