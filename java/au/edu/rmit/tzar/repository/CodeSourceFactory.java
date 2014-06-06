package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.TzarException;

import java.net.URI;

/**
 * Factory methods for creating CodeSource implementations.
 */
public class CodeSourceFactory {
  public static CodeSourceImpl createCodeSource(String revision, CodeSourceImpl.RepositoryTypeImpl repositoryType,
      URI sourceUri) throws TzarException, CodeSourceImpl.InvalidRevisionException {
    if (revision.equals(Constants.HEAD_REVISION)) {
      revision = repositoryType.createRepository(sourceUri).getHeadRevision();
    } else {
      if (!repositoryType.isValidRevision(revision)) {
        throw new CodeSourceImpl.InvalidRevisionException(revision, repositoryType);
      }
    }

    return new CodeSourceImpl(sourceUri, repositoryType, revision);
  }
}
