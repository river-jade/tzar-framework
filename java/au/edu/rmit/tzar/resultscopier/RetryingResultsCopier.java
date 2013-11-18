package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.base.Optional;

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
  public void copyResults(Run run, File sourcePath, boolean success) throws TzarException {
    Optional<TzarException> e = Optional.absent();
    for (int i = 0; i < retryCount; i++) {
      if (e.isPresent()) {
        LOG.log(Level.WARNING, "Error copying results for run: " + run + ". Error: " + e.get().getMessage() +
            " Retry: " + i);
      }

      e = copyResultsInternal(run, sourcePath, success);
      if (!e.isPresent()) { // no error, success!
        return;
      }
    }
    throw e.get();
  }

  private Optional<TzarException> copyResultsInternal(Run run, File sourcePath, boolean success) {
    try {
      delegate.copyResults(run, sourcePath, success);
      return Optional.absent();
    } catch (TzarException e) {
      return Optional.of(e);
    }
  }
}

