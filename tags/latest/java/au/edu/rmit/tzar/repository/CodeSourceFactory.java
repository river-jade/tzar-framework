package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.parser.beans.DownloadMode;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;

/**
 * Factory methods for creating CodeSource implementations.
 */
public class CodeSourceFactory {
  private final CloseableHttpClient cachingHttpClient;
  private final CloseableHttpClient nonCachingHttpClient;

  public CodeSourceFactory(CloseableHttpClient cachingHttpClient, CloseableHttpClient nonCachingHttpClient) {
    this.cachingHttpClient = cachingHttpClient;
    this.nonCachingHttpClient = nonCachingHttpClient;
  }

  public CodeSourceImpl createCodeSource(String revision, CodeSourceImpl.RepositoryTypeImpl repositoryType,
      URI sourceUri, DownloadMode downloadMode) throws TzarException {
    if (revision.equals(Constants.HEAD_REVISION)) {
      boolean downloadOnce = downloadMode == DownloadMode.ONCE;
      revision = repositoryType.createRepository(cachingHttpClient, sourceUri, downloadOnce).getHeadRevision();
    } else {
      if (!repositoryType.isValidRevision(revision)) {
        throw new CodeSourceImpl.InvalidRevisionException(revision, repositoryType);
      }
    }

    CloseableHttpClient httpClient;
    if (downloadMode == DownloadMode.CACHE) {
      httpClient = cachingHttpClient;
    } else {
      httpClient = nonCachingHttpClient;
    }
    return new CodeSourceImpl(httpClient, sourceUri, repositoryType, revision, downloadMode);
  }
}
