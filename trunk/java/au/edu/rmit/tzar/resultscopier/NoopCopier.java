package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;

/**
 * No-op results copier. Does nothing. For use when no final output path is specified.
 */
public class NoopCopier implements ResultsCopier {
  @Override
  public void copyResults(Run run, File sourcePath, boolean success) throws TzarException {
  }
}
