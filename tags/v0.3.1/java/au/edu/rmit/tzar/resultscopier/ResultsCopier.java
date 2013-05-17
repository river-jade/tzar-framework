package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;

import java.io.File;

/**
 * Copies results from local storage to permanent storage.
 */
public interface ResultsCopier {
  /**
   * Copies the sourcePath directory to the destination path for this copier. The destination path
   * may be on another server, copied by a protocol specific to the implementation of this interface.
   * Implementations should update outputHost and outputPath on the run after copying.
   *
   *
   * @param run        the run containing the results to be copied
   * @param sourcePath the path containing the run results
   * @param success    true if the run was successful
   * @throws RdvException if the files could not be copied
   */
  void copyResults(Run run, File sourcePath, boolean success) throws RdvException;

  /**
   * Gets the base destination path for this copier.
   */
  File getBaseDestPath();
}
