package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.parser.beans.DownloadMode;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class CodeSourceFactoryTest extends TestCase {
  private CloseableHttpClient cachingHttpClient = mock(CloseableHttpClient.class);
  private CloseableHttpClient nonCachingHttpClient = mock(CloseableHttpClient.class);
  private CodeSourceFactory codeSourceFactory;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    codeSourceFactory = new CodeSourceFactory(cachingHttpClient, nonCachingHttpClient);
  }

  public void testCreateCodeSourceCached() throws URISyntaxException, TzarException, IOException {
    CodeSourceImpl codeSource = codeSourceFactory.createCodeSource("revision", CodeSourceImpl.RepositoryTypeImpl
            .HTTP_FILE, new URI("abcdef"), DownloadMode.CACHE);

    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    when(cachingHttpClient.execute(isA(HttpGet.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK,
        ""));
    when(response.getEntity()).thenReturn(mock(HttpEntity.class));
    codeSource.getCode(Files.createTempDir(), "name");
  }

  public void testCreateCodeSourceNonCached() throws URISyntaxException, TzarException, IOException {
    CodeSourceImpl codeSource = codeSourceFactory.createCodeSource("revision", CodeSourceImpl.RepositoryTypeImpl
            .HTTP_FILE, new URI("abcdef"), DownloadMode.FORCE);

    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    when(nonCachingHttpClient.execute(isA(HttpGet.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK,
        ""));
    when(response.getEntity()).thenReturn(mock(HttpEntity.class));
    codeSource.getCode(Files.createTempDir(), "name");
  }

  public void testCreateCodeSourceOnce() throws URISyntaxException, TzarException, IOException {
    CodeSourceImpl codeSource = codeSourceFactory.createCodeSource("revision", CodeSourceImpl.RepositoryTypeImpl
            .HTTP_FILE, new URI("abcdef"), DownloadMode.ONCE);

    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    when(nonCachingHttpClient.execute(isA(HttpGet.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK,
        ""));
    when(response.getEntity()).thenReturn(mock(HttpEntity.class));
    File tempDir = Files.createTempDir();
    codeSource.getCode(tempDir, "name");
    codeSource.getCode(tempDir, "name");
    verify(nonCachingHttpClient, times(1)).execute(isA(HttpGet.class));
  }
}
