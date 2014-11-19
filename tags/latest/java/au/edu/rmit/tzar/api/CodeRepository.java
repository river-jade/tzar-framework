package au.edu.rmit.tzar.api;

import java.io.File;

/**
 * Returns a file path for a given code revision. Implementations may choose to ignore
 * the revision number if not relevant.
 */
public interface CodeRepository {
  /**
   * Checks out the model code from code repository into a local directory at the provided revision.
   *
   * @param revision the version of the model / framework to load
   * @param name a user-meaningful name for this library (this will be used to name the
   *             directory where it is saved
   * @param modelPath base path at which to store the download model
   * @return the path to the cached model / framework code
   * @throws TzarException if an error occurs contacting the repository
   */
  File retrieveModel(String revision, String name, File modelPath) throws TzarException;

  /**
   * Checks out the project params file from the repository at the given revision.
   * @param projectParamFilename filename of the project params
   * @param revision the revision to retrieve
   * @param destPath base path at which to store the project params file
   * @return the local path to the retrieved file
   * @throws TzarException if an error occurs retrieving the file
   */
  File retrieveProjectParams(String projectParamFilename, String revision, File destPath) throws TzarException;

  /**
   * Gets the revision of current head for this repository. For non-versioned repositories,
   * this has no semantic meaning and should be the empty string.
   * @return the revision. This will be a number for subversion, but may be a string for other
   * repositories, eg git.
   * @throws TzarException if an error occurs contacting the repository
   */
  String getHeadRevision() throws TzarException;
}
