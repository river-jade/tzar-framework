package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ResultsCopier decorator which accepts copy requests and adds them to a
 * threadsafe queue. Runs a separate thread to actually copy the results.
 * The task of copying the results is delegated to another implementation
 * of ResultsCopier.
 */
public class AsyncResultsCopier implements ResultsCopier, Runnable {
  private static final Logger LOG = Logger.getLogger(AsyncResultsCopier.class.getName());

  private final BlockingQueue<RunAndPath> copyQueue = new LinkedBlockingQueue<RunAndPath>();
  private final ResultsCopier delegate;

  public AsyncResultsCopier(ResultsCopier delegate) {
    this.delegate = delegate;
  }

  @Override
  public void copyResults(Run run, File sourcePath) {
    copyQueue.add(new RunAndPath(run, sourcePath));
  }

  @Override
  public File getBaseDestPath() {
    return delegate.getBaseDestPath();
  }

  @Override
  public void run() {
    while (true) {
      RunAndPath runAndPath;
      try {
        runAndPath = copyQueue.take();
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, "Copy thread was interrupted.", e);
        Thread.currentThread().interrupt();
        return;
      }

      Run run = runAndPath.run;

      try {
        delegate.copyResults(run, runAndPath.path);
      } catch (RdvException e) {
        LOG.log(Level.WARNING, "Error copying results for run: " + run, e);
      }
    }
  }

  private class RunAndPath {
    private final Run run;
    private final File path;

    private RunAndPath(Run run, File path) {
      this.run = run;
      this.path = path;
    }
  }
}
