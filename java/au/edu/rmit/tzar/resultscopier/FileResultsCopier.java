package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Copies results using the local file system (including mounted remote drives).
 */
public class FileResultsCopier implements ResultsCopier {
  private static Logger LOG = Logger.getLogger(FileResultsCopier.class.getName());

  private final File baseDestPath;

  /**
   * Constructor.
   *
   * @param baseDestPath the path for the results to be copied to. This directory
   *                     will be created if it does not exist.
   * @throws IOException if the base destination path already exists and is not a directory
   *                     or does not exist and cannot be created
   */
  public FileResultsCopier(File baseDestPath) throws IOException {
    this.baseDestPath = baseDestPath;
    if (!baseDestPath.exists()) {
      if (!baseDestPath.mkdirs()) {
        throw new IOException("Could not make directory: " + baseDestPath);
      }
    } else if (!baseDestPath.isDirectory()) {
      throw new IOException("Destination path <" + baseDestPath + "> exists and is not a directory.");
    }
  }

  @Override
  public void copyResults(Run run, File sourcePath, boolean success) throws TzarException {
    if (!sourcePath.isDirectory()) {
      throw new TzarException("Source path was not a directory.");
    }
    File dest = new File(baseDestPath, sourcePath.getName());
    dest.mkdir();
    LOG.info("Copying results from: " + sourcePath + " to " + dest);
    try {
      Utils.copyDirectory(sourcePath, dest);
    } catch (IOException e) {
      throw new TzarException(e);
    }
    run.setOutputHost(Utils.getHostname());
    run.setRemoteOutputPath(dest);
  }
}
