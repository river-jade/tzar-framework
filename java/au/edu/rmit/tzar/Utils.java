package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import com.google.common.io.Files;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility functions.
 */
public class Utils {
  private static final Logger LOG = Logger.getLogger(Utils.class.getName());

  private Utils() { // not to be instantiated
  }

  public static String getHostname() {
    try {
      // This is the obvious way to retrieve the hostname. Unfortunately, if the /etc/hosts file
      // isn't correctly populated (which is the case on the Nectar nodes we've been using, this
      // will throw an UnknownHostException. As such we use a hacky workaround.
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      try {
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
          for (InetAddress ia : Collections.list(iface.getInetAddresses())) {
            if (!ia.isLoopbackAddress() && (!ia.isLinkLocalAddress()) && (ia instanceof Inet4Address))
              return ia.getHostName();
          }
        }
      } catch (SocketException e1) {
      }
      LOG.warning("Couldn't find the hostname.");
      return "UNKNOWN";
    }
  }

  /**
   * Spawns a new thread to copy an InputStream to the logger at the provided level.
   *
   * @param in       stream to copy
   * @param logger   logger to log to
   * @param logLevel logging level to log at
   */
  public static void copyStreamToLog(final InputStream in, final Logger logger, final Level logLevel) {
    new Thread() {
      @Override
      public void run() {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(in));
          String line;
          while ((line = br.readLine()) != null) {
            logger.log(logLevel, line);
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to copy provided stream to logs.", e);
        }
      }
    }.start();
  }

  public static void copyDirectory(File source, File dest) throws IOException {
    copyDirectory(source, null, dest, new NoopRenamer());
  }

  public static void copyDirectory(File source, File dest, RenamingStrategy renamer) throws IOException {
    copyDirectory(source, null, dest, renamer);
  }

  /**
   * Wrapper for File.moveTo. Required because there are known issues with this method on
   * Windoze systems. Windows cannot rename a file if any process has the file open for
   * reading/writing. There is also apparently an unpredictable delay after closing a file
   * handle in which the move operation will still fail (!!!). This method tries up to 10
   * times to rename the directory, waiting longer each time (up to 30 seconds).
   *
   * @param source source to rename
   * @param dest   new name / path
   * @throws RdvException if the file / directory cannot be renamed.
   */
  public static void fileRename(File source, File dest) throws RdvException {
    LOG.info("Renaming \"" + source + "\" to \"" + dest + "\"");
    for (int i = 0; i < 10; i++) {
      if (source.renameTo(dest)) { // success!
        return;
      }
      long delay = (long) Math.pow(1.4, i) * 1000;
      LOG.warning("Renaming \"" + source + "\" to \"" + dest + "\" failed. Waiting " +
          delay + " seconds and retrying.");
      try {
        Thread.sleep(delay); // exponential backoff, first wait is 1 second, last wait is 30s.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RdvException(e);
      }
    }
    throw new RdvException("Unable to rename \"" + source + "\" to \"" + dest + "\"");
  }

  private static void copyDirectory(File sourceBase, File sourceRel, File destBase, RenamingStrategy renamer)
      throws IOException {
    File sourcePath;
    if (sourceRel == null) {
      sourcePath = sourceBase;
    } else {
      sourcePath = new File(sourceBase, sourceRel.getPath());
    }
    if (!sourcePath.exists()) {
      LOG.warning("Path " + sourcePath + " does not exist!");
      return;
    }
    if (sourcePath.isDirectory()) {
      for (String child : sourcePath.list()) {
        copyDirectory(sourceBase, new File(sourceRel, child), destBase, renamer);
      }
    } else {
      File destRel = renamer.rename(sourceRel);
      File destPath = new File(destBase, destRel.getPath());
      destPath.getParentFile().mkdirs();
      Files.copy(sourcePath, destPath);
    }
  }

  public interface RenamingStrategy {
    /**
     * Given a run and a (relative) file source path, returns a (relative)
     * destination path for the provided file. An example strategy might be
     * to remap a/b/c to a/b/<run_id>.c or perhaps a/b/c to a_b_c_<hostname>_<run_id>
     *
     * @param file the file to be copied and potentially renamed
     * @return the new relative path for the file.
     */
    File rename(File file);
  }

  /**
   * Do nothing renamer.
   */
  private static class NoopRenamer implements RenamingStrategy {
    @Override
    public File rename(File file) {
      return file;
    }
  }
}

