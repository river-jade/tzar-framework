package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Represents a resource stored on an http server. As http is not
 * a versioned protocol, revision parameters are ignored.
 */
public class HttpRepository extends UriRepository {
  private static final Logger LOG = Logger.getLogger(HttpRepository.class.getName());
  @VisibleForTesting
  final CloseableHttpClient client;

  public HttpRepository(File baseModelsPath, URI sourceUri) {
    super(baseModelsPath, sourceUri);
    client = CachingHttpClientBuilder.create().setCacheConfig(CacheConfig.DEFAULT).build();
    // the line below should do the same as the line above, but there seems to be a bug which
    // causes a null pointer exception.
    // client = CachingHttpClients.createMemoryBound();
  }

  @Override
  public File retrieveModel(String revision) throws TzarException {
    LOG.info(String.format("Retrieving model from %s",  sourceUri));
    return retrieveFile(sourceUri);
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision) throws TzarException {
    File tempDir = Files.createTempDir();

    URI uri;
    try {
      uri = new URIBuilder(sourceUri).setPath(sourceUri.getPath() + "/" + projectParamFilename).build();
    } catch (URISyntaxException e) {
      throw new TzarException(e);
    }
    LOG.info(String.format("Retrieving project.yaml from: %s to local path: %s", uri, tempDir));
    return retrieveFile(uri);
  }

  @Override
  public String getHeadRevision() throws TzarException {
    return "";
  }

  private File retrieveFile(URI sourceUri) throws TzarException {
    try {
      CloseableHttpResponse response = client.execute(new HttpGet(sourceUri));
      boolean exceptionOccurred = true;
      try {
        HttpEntity entity = response.getEntity();
        modelPath.mkdir();
        File outputFile = File.createTempFile("httpRepository", null, modelPath);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        entity.writeTo(fileOutputStream);
        exceptionOccurred = false;
        return outputFile;
      } finally {
        Closeables.close(response, exceptionOccurred);
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }
}
