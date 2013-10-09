package au.edu.rmit.tzar.api;

import java.io.File;
import java.util.logging.Logger;

/**
 * This is the interface which projects need to implement and deploy. The implementation class will
 * be dynamically loaded by the framework, and the runModel method called.
 */
public interface Runner {
  /**
   * Runs the provided model code. "model" points to a local source repository.
   * Implementations of this runner are expected to be able to run code found at
   * this location.
   *
   * @param model      the path to the model source code or executable
   * @param outputPath local path for the model to write output data to
   * @param runId      a unique identifier for the run to execute
   * @param runnerFlags space separated flags for the runner
   * @param parameters
   * @param logger     a java logger for the runner code to use for logging. This logger
   * will log to a file in the output directory. By default, INFO level logs will go to
   * console. If --verbose is set, then FINE logs will also go to console.
   * @return true if the run succeeded, false otherwise
   * @throws TzarException
   */
  // TODO(michaell): read flags from project spec
  boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger) throws TzarException;
}
