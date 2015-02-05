package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
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
  public void copyResults(final Run run, final File sourcePath, final boolean success) throws TzarException {
    Utils.Retryable.retryWithBackoff(retryCount, 1000, new Utils.Retryable() {
      @Override
      public void exec() throws TzarException {
        delegate.copyResults(run, sourcePath, success);
      }
    });
  }
}

