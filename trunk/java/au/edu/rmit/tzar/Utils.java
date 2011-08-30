package au.edu.rmit.tzar;

import com.google.common.io.Files;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility functions.
 */
public class Utils {
  private Utils() { // not to be instantiated
  }

  public static String getHostname() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
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

  private static void copyDirectory(File sourceBase, File sourceRel, File destBase, RenamingStrategy renamer)
      throws IOException {
    File sourcePath;
    if (sourceRel == null) {
      sourcePath = sourceBase;
    } else {
      sourcePath = new File(sourceBase, sourceRel.getPath());
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

