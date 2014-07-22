package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.TzarException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;

/**
 * Factory methods for creating CodeSource implementations.
 */
public class CodeSourceFactory {
  private final CloseableHttpClient httpClient;

  public CodeSourceFactory(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public CodeSourceImpl createCodeSource(String revision, CodeSourceImpl.RepositoryTypeImpl repositoryType,
      URI sourceUri, boolean forceDownload) throws TzarException {
    if (revision.equals(Constants.HEAD_REVISION)) {
      revision = repositoryType.createRepository(httpClient, sourceUri, forceDownload).getHeadRevision();
    } else {
      if (!repositoryType.isValidRevision(revision)) {
        throw new CodeSourceImpl.InvalidRevisionException(revision, repositoryType);
      }
    }

    return new CodeSourceImpl(httpClient, sourceUri, repositoryType, revision, forceDownload);
  }
}
