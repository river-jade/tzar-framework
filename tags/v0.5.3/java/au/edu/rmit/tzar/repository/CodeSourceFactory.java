package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.net.URI;

/**
 * Factory methods for creating CodeSource implementations.
 */
public class CodeSourceFactory {
  public static CodeSourceImpl createCodeSource(String revision, CodeSourceImpl.RepositoryTypeImpl repositoryType,
      URI sourceUri, File baseModelPath) throws TzarException, CodeSourceImpl.InvalidRevisionException {
    if (revision.equals(Constants.HEAD_REVISION)) {
      revision = repositoryType.createRepository(sourceUri, baseModelPath).getHeadRevision();
    } else {
      if (!repositoryType.isValidRevision(revision)) {
        throw new CodeSourceImpl.InvalidRevisionException(revision, repositoryType);
      }
    }

    return new CodeSourceImpl(sourceUri, repositoryType, revision);
  }
}
