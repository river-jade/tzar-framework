package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

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
public class HttpRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(HttpRepository.class.getName());

  @VisibleForTesting
  final CloseableHttpClient client;
  private final boolean downloadOnce;

  /**
   * @param httpClient
   * @param sourceUri the URL to download from
   * @param downloadOnce whether to use locally cached copy of the downloaded files
   */
  public HttpRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
    super(sourceUri);
    this.downloadOnce = downloadOnce;
    client = httpClient;
  }

  @Override
  public File retrieveModel(String revision, String name, File baseModelPath) throws TzarException {
    File modelPath = createModelPath(name, baseModelPath, sourceUri);
    LOG.info(String.format("Retrieving model from %s to %s", sourceUri, modelPath));
    if (downloadOnce && modelPath.exists()) {
      LOG.info(String.format("Model already exists at %s so not downloading", modelPath));
    } else {
      retrieveFile(modelPath);
    }
    return modelPath;
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision, File destPath) throws TzarException {
    File tempDir = Files.createTempDir();

    URI uri;
    try {
      uri = new URIBuilder(sourceUri).setPath(sourceUri.getPath() + "/" + projectParamFilename).build();
    } catch (URISyntaxException e) {
      throw new TzarException(e);
    }
    LOG.fine(String.format("Retrieving project.yaml from: %s to local path: %s", uri, tempDir));
    File path = createModelPath("project_params", destPath, sourceUri);
    retrieveFile(path);
    return path;
  }

  @Override
  public String getHeadRevision() throws TzarException {
    return "";
  }

  void retrieveFile(File outputFile) throws TzarException {
    try {
      CloseableHttpResponse response = client.execute(new HttpGet(sourceUri));
      boolean exceptionOccurred = true;
      try {
        HttpEntity entity = response.getEntity();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
          throw new TzarException(String.format("Http error retrieving file from URL: %s. Error was: %s", sourceUri,
              response.getStatusLine()));
        }
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        entity.writeTo(fileOutputStream);
        exceptionOccurred = false;
      } finally {
        Closeables.close(response, exceptionOccurred);
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }
}
