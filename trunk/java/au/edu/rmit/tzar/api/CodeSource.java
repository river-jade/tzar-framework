package au.edu.rmit.tzar.api;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A CodeSource represents a place from which a Tzar project or a library can be loaded.
 */
public interface CodeSource {
  /**
   * Retrieves the project's model code from the repository and writes it to baseModelPath.
   * @param baseModelPath the local file path at which to write the downloaded data
   * @return the path to the downloaded code
   * @throws TzarException
   */
  File getCode(File baseModelPath) throws TzarException;

  /**
   * Retrieves the project's specification from the repository and writes it to baseModelPath.
   * @param baseModelPath the local file path at which to write the downloaded data
   * @return the project specification
   * @throws TzarException
   * @throws FileNotFoundException
   */
  ProjectSpec getProjectSpec(File baseModelPath) throws TzarException, FileNotFoundException;
}
