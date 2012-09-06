package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
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
  public void copyResults(Run run, File sourcePath) throws RdvException {
    if (!sourcePath.isDirectory()) {
      throw new RdvException("Source path was not a directory.");
    }
    File dest = new File(baseDestPath, sourcePath.getName());
    dest.mkdir();
    LOG.info("Copying results from: " + sourcePath + " to " + dest);
    try {
      Utils.copyDirectory(sourcePath, dest);
    } catch (IOException e) {
      throw new RdvException(e);
    }
    String hostname;
    try {
      hostname = Utils.getHostname();
    } catch (UnknownHostException e) {
      hostname = "UNKNOWN";
    }
    run.setOutputHost(hostname);
    run.setOutputPath(dest);
  }

  @Override
  public File getBaseDestPath() {
    return baseDestPath;
  }

}
