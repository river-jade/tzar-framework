package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.TzarException;
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

  private final BlockingQueue<CopyJob> copyQueue = new LinkedBlockingQueue<CopyJob>();
  private final ResultsCopier delegate;

  public AsyncResultsCopier(ResultsCopier delegate) {
    this.delegate = delegate;
  }

  @Override
  public void copyResults(Run run, File sourcePath, boolean success) {
    copyQueue.add(new CopyJob(run, sourcePath, success));
  }

  @Override
  public File getBaseDestPath() {
    return delegate.getBaseDestPath();
  }

  @Override
  public void run() {
    while (true) {
      CopyJob copyJob;
      try {
        copyJob = copyQueue.take();
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, "Copy thread was interrupted.", e);
        Thread.currentThread().interrupt();
        return;
      }

      Run run = copyJob.run;

      try {
        delegate.copyResults(run, copyJob.path, copyJob.success);
      } catch (TzarException e) {
        LOG.log(Level.WARNING, "Error copying results for run: " + run, e);
      }
    }
  }

  private class CopyJob {
    private final Run run;
    private final File path;
    private final boolean success;

    private CopyJob(Run run, File path, boolean success) {
      this.run = run;
      this.path = path;
      this.success = success;
    }
  }
}
