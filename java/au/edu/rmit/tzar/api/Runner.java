package au.edu.rmit.tzar.api;

import java.io.File;

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
   * @param flags      flags to pass to the model
   * @param parameters
   * @return true if the run succeeded, false otherwise
   * @throws RdvException
   */
  // TODO(michaell): read flags from project spec
  boolean runModel(File model, File outputPath, String runId, String flags, Parameters parameters) throws RdvException;
}
