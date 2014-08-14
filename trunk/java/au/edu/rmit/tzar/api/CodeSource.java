package au.edu.rmit.tzar.api;

import au.edu.rmit.tzar.parser.beans.DownloadMode;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import org.apache.http.impl.client.CloseableHttpClient;

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
   * @param codeSourceFactory used to spawn new code source objects for downloading libraries
   * @param projectFileName name of the file containing the project specification. If string is empty then the default at Constants.PROJECT_YAML is used.
   * @return the project specification
   * @throws TzarException
   * @throws FileNotFoundException
   */
  ProjectSpec getProjectSpec(File baseModelPath, CodeSourceFactory codeSourceFactory, String projectFileName)
      throws TzarException, FileNotFoundException;

  /**
   * The revision that this CodeSource points to. #getCode and #getProjectSpec will retrieve
   * code at this revision. This value is meaningless for CodeSources which do not represent a versioned
   * file system / VCS and will be ignored. Should not be null.
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

  /**
   * Determines if we should re-download this codesource even if it already exists locally.
   * @return true if we should always re-download
   */
  DownloadMode getDownloadMode();

  public interface RepositoryType {
    CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce);
    boolean isValidRevision(String revision);
  }
}
