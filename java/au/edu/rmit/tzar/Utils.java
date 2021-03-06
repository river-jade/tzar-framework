package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.PathUtils;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static utility functions.
 */
public class Utils {
  private static final Logger LOG = Logger.getLogger(Utils.class.getName());

  private Utils() { // not to be instantiated
  }

  /**
   * Returns the default value if str is null or of zero length, str otherwise.
   * @param str string to test and return if non-empty / null
   * @param def default to return if str is empty or null
   * @return str or def if str is empty or null
   */
  public static String defaultIfEmpty(String str, String def) {
    return (str == null || str.isEmpty()) ?  def : str;
  }

  public static String getHostname() {
    try {
      // This is the canonical way to retrieve the hostname. Unfortunately, if the /etc/hosts file
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

  public static String getHostIp() {
    try {
      // This is the canonical way to retrieve the host ip. Unfortunately, if the /etc/hosts file
      // isn't correctly populated (which is the case on the Nectar nodes we've been using, this
      // will throw an UnknownHostException. As such we use a hacky workaround.
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      try {
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
          for (InetAddress ia : Collections.list(iface.getInetAddresses())) {
            if (!ia.isLoopbackAddress() && (!ia.isLinkLocalAddress()) && (ia instanceof Inet4Address))
              return ia.getHostAddress();
          }
        }
      } catch (SocketException e1) {
      }
      LOG.warning("Couldn't find the host ip address.");
      return "UNKNOWN";
    }
  }

  /**
   * Spawns a new thread to copy an InputStream to the logger at the provided level. This method
   * registers a stopTask to the provided StopRun object so that the logger can be gracefully disconnected
   * before the input stream becomes unavailable.
   *
   * @param in       stream to copy
   * @param logger   logger to log to
   * @param logLevel logging level to log at
   * @param stopRun  object to register tasks to be executed if a run is to be stopped mid-run
   * @param executor executor service for spawning new threads
   */
  public static void copyStreamToLog(final InputStream in, final Logger logger, final Level logLevel,
      final StopRun stopRun, ExecutorService executor) {
    final CountDownLatch finished = new CountDownLatch(1);
    final AtomicBoolean stopLogging = new AtomicBoolean(false);

    // this task will run iff stopRun.stop() is called, which will be done if the user
    // requests stopping a run in mid-run.
    stopRun.registerStopTask(new Runnable() {
      @Override
      public void run() {
        stopLogging.set(true); // tell the logger to stop
        try {
          finished.await(); // wait for the logger to stop (to avoid a race condition) before continuing with the
                            // other stop tasks
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
        }
      }
    });
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(in));
          String line;
          while (!stopLogging.get() && (line = br.readLine()) != null) {
            logger.log(logLevel, line);
          }
          finished.countDown(); // let the stop task know that we're done
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to copy provided stream to logs.", e);
        }
      }
    });
  }

  public static void copyDirectory(File source, File dest) throws IOException {
    copyDirectory(source, null, dest, new NoopRenamer(), new AllFilesFilter());
  }

  public static void copyDirectory(File source, File dest, RenamingStrategy renamer, Filter filter)
      throws IOException {
    copyDirectory(source, null, dest, renamer, filter);
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
   * @throws TzarException if the file / directory cannot be renamed.
   */
  public static void fileRename(File source, File dest) throws TzarException {
    LOG.fine("Renaming \"" + source + "\" to \"" + dest + "\"");
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
        throw new TzarException(e);
      }
    }
    throw new TzarException("Unable to rename \"" + source + "\" to \"" + dest + "\"");
  }

  /**
   * Recursive copy of a directory with renaming and filtering.
   *
   * @param sourceBase base directory to copy the source files from
   * @param sourceRel relative directory to copy the source files from. We separate the
   * base and relative directories to allow the renaming strategy to alter (eg flatten)
   * the directory structure.
   * @param destBase base output path at the destination
   * @param renamer strategy to use for renaming files
   * @param filter filter to determine whether to include files / directories
   * @throws IOException if the files can't be read / written
   */
  private static void copyDirectory(File sourceBase, File sourceRel, File destBase, RenamingStrategy renamer,
      Filter filter) throws IOException {
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
        copyDirectory(sourceBase, new File(sourceRel, child), destBase, renamer, filter);
      }
    } else {
      if (!filter.matches(sourceRel)) {
        return;
      }
      File destRel = renamer.rename(sourceRel);
      File destPath = new File(destBase, destRel.getPath());
      destPath.getParentFile().mkdirs();
      Files.copy(sourcePath, destPath);
    }
  }

  /**
   * Delete a file or directory recursively. Note that this method is not threadsafe.
   * If another process modifies the tree to be deleted, this method could fail.
   * There is no way to do this in a threadsafe /and/ OS independent way.
   * @param root the root of the tree to be deleted, /not/ the file system root!
   * @throws IOException
   */
  public static void deleteRecursively(File root) throws IOException {
    for (File file : Files.fileTreeTraverser().postOrderTraversal(root)) {
      if (!file.delete()) {
        throw new IOException("Couldn't delete file: " + file);
      }
    }
  }

  /**
   * Creates a URI from a string. If the provided string has no scheme (eg http, ftp), we
   * assume it's a file path and set the scheme to "file".
   *
   * @param uriString the string to convert to a URI
   * @return a newly created URI object
   */
  public static URI makeAbsoluteUri(String uriString) {
    final URI uri;
    try {
      uri = new URI(uriString);
    } catch (URISyntaxException e) {
      // this may be a windows path, so we'll try creating a File object.
      return new File(uriString).toURI();
    }
    if (uri.getScheme() == null) { // no scheme (eg http, ftp). assuming it's a file path
      return new File(uriString).toURI();
    } else {
      return uri;
    }
  }

  public static <V, I> V reduce(Iterable<I> iter, Function<Pair<V, I>, V> combiner, V initial) {
    V currentVal = initial;
    for (I val : iter) {
      currentVal = combiner.apply(new Pair<V, I>(currentVal, val));
    }
    return currentVal;
  }

  /**
   * Generates the output path for the runset, based on the tzar output path (the base output path for
   * all run data), the project name, and the runset name.
   */
  public static File createRunsetOutputPath(File tzarOutputPath, String projectName, String runset) {
    return new File(tzarOutputPath, PathUtils.combineAndReplaceWhitespace("_", projectName, runset));
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
   * A filter that determines whether a filename matches and should be included.
   */
  public interface Filter {
    /**
     * Returns true if the file matches and should be copied.
     * @param file
     * @return
     */
    boolean matches(File file);
  }

  /**
   * Do nothing renamer.
   */
  public static class NoopRenamer implements RenamingStrategy {
    @Override
    public File rename(File file) {
      return file;
    }
  }

  public static class Pair<V1, V2> {
    public final V1 first;
    public final V2 second;

    private Pair(V1 first, V2 second) {
      this.first = first;
      this.second = second;
    }

    public static <V1, V2> Pair<V1, V2> of(V1 first, V2 second) {
      return new Pair<V1, V2>(first, second);
    }
  }

  public static class AllFilesFilter implements Filter {
    @Override
    public boolean matches(File file) {
      return true;
    }
  }

  public static class RegexFilter implements Utils.Filter {
    private final Optional<Pattern> filenamePattern;

    public static RegexFilter of(Optional<String> filenameFilter) {
      return new RegexFilter(filenameFilter.transform(new Function<String, Pattern>() {
        public Pattern apply(String input) {
          return Pattern.compile(input);
        }
      }));
    }

    private RegexFilter(Optional<Pattern> pattern) {
      filenamePattern = pattern;
    }

    @Override
    public boolean matches(File file) {
      if (!filenamePattern.isPresent()) {
        return true;
      }
      Matcher matcher = filenamePattern.get().matcher(file.toString());
      return matcher.matches();
    }
  }

  /**
   * Utility class which retries a method with exponential backoff.
   */
  public static abstract class Retryable {
    private static Logger LOG = Logger.getLogger(Retryable.class.getName());

    /**
     * Executes the exec method of the passed Retryable object. If it throws a TzarException,
     * retry the method [retryCount] times, pausing for initialSleepTimeMillis after the
     * first failure, and doubling the sleep time between each attempt.
     * @param retryCount number of times to retry
     * @param initialSleepTimeMillis number of millisecods to pause after the first attempt
     * @param retryable the object containing the method to retry (usually an anonymous wrapper class)
     * @throws TzarException if the method fails for each of the retry attempts
     */
    public static void retryWithBackoff(int retryCount, int initialSleepTimeMillis, Retryable retryable)
        throws TzarException {
      int sleepTime = initialSleepTimeMillis;
      TzarException lastException = null;
      for (int i = 1; i <= retryCount; i++) {
        try {
          retryable.exec();
          return;
        } catch (TzarException e) {
          LOG.log(Level.WARNING, "Exception occurred, pausing for {0}s and retrying. Retry #{1} of {2}. " +
              "Error was: {3}", new Object[]{sleepTime/1000, i, retryCount, e.getMessage()});
          lastException = e;
          try {
            Thread.sleep(sleepTime);
            sleepTime *= 2;
          } catch (InterruptedException e1) {
            throw new TzarException(e1);
          }
        }
      }
      throw lastException;
    }

    public abstract void exec() throws TzarException;
  }

  public static CloseableHttpClient createHttpClient(File cacheDir) {
    // some websites give different responses, depending on the user agent. We gamble here
    // that we'll get the most sane response for something that doesn't look like a browser.
    String USER_AGENT = "Wget/1.12";

    CacheConfig cacheConfig = CacheConfig.custom().setMaxObjectSize(Constants.MAX_CACHE_OBJECT_SIZE_BYTES).build();
    return CachingHttpClients.custom()
      .setCacheConfig(cacheConfig)
      .setCacheDir(cacheDir)
      .setUserAgent(USER_AGENT)
      .useSystemProperties()
      .build();
  }

  public static CloseableHttpClient createNonCachingHttpClient() {
    return HttpClientBuilder.create().build();
  }
}
