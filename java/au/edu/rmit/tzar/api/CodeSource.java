package au.edu.rmit.tzar.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * A CodeSource represents a place from which a Tzar project or a library can be loaded.
 */
public interface CodeSource {
  /**
   * Retrieves the project's model code from the repository and writes it to baseModelPath.
   *
   * @param baseModelPath the local file path at which to write the downloaded data
   * @param name a name for this code source that has some meaning for the user
   * @return the path to the downloaded code
   * @throws TzarException
   */
  File getCode(File baseModelPath, String name) throws TzarException;

  /**
   * Retrieves the project's specification from the repository and writes it to baseModelPath.
   * @param baseModelPath the local file path at which to write the downloaded data
   * @return the project specification
   * @throws TzarException
   * @throws FileNotFoundException
   */
  ProjectSpec getProjectSpec(File baseModelPath) throws TzarException, FileNotFoundException;

  /**
   * The revision that this CodeSource points to. #getCode and #getProjectSpec will retrieve
   * code at this revision. This value is meaningless for CodeSources which do not represent a versioned
   * file system / VCS.
   * @return the revision
   */
  String getRevision();

  /**
   * The URI that this CodeSource points to. #getCode and #getProjectSpec will retrieve
   * code from this location. Acceptable types of URI will vary depending on the code source
   * type and implementation.
   * @return the revision
   */
  URI getSourceUri();

  CodeSource.RepositoryType getRepositoryType();

  public interface RepositoryType {
    CodeRepository createRepository(URI sourceUri);
    boolean isValidRevision(String revision);
  }
}
