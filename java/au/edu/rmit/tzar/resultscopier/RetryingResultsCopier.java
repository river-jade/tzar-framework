package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorator for ResultsCopiers that retries failed copy attempts.
 */
public class RetryingResultsCopier implements ResultsCopier {
  private static final Logger LOG = Logger.getLogger(RetryingResultsCopier.class.getName());

  private final ResultsCopier delegate;
  private final int retryCount;

  public RetryingResultsCopier(ResultsCopier delegate, int retryCount) {
    this.delegate = delegate;
    this.retryCount = retryCount;
  }

  @Override
  public void copyResults(Run run, File sourcePath) throws RdvException {
    RdvException e = null;
    for (int i = 0; i < retryCount; i++) {
      if (e != null) {
        LOG.log(Level.WARNING, "Error copying results for run: " + run + ". Retry: " + i, e);
      }

      e = copyResultsInternal(run, sourcePath);
      if (e == null) { // success!
        return;
      }
    }
    throw e;
  }

  private RdvException copyResultsInternal(Run run, File sourcePath) {
    try {
      delegate.copyResults(run, sourcePath);
      return null;
    } catch (RdvException e) {
      return e;
    }
  }

  @Override
  public File getBaseDestPath() {
    return delegate.getBaseDestPath();
  }
}

