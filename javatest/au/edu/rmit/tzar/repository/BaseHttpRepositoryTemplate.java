package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for Http and HttpZip repository tests.
 */
public abstract class BaseHttpRepositoryTemplate extends TestCase {
  public static final String EXPECTED = "some text downloaded from the web";
  public static final String REVISION = "";

  protected HttpRepository repository;
  protected URI sourceUri;
  protected File baseModelPath;
  protected CloseableHttpClient mockHttpClient;
  protected ByteArrayEntity returnedByteArray;

  public void setUp() throws Exception {
    mockHttpClient = mock(CloseableHttpClient.class);
    sourceUri = new URI("http://some.com/a_url");
    baseModelPath = Files.createTempDir();
  }

  protected File retrieveModel(int statusCode) throws IOException, TzarException {
    CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
    when(mockHttpClient.execute(isA(HttpGet.class))).thenReturn(mockResponse);
    when(mockResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("", 0, 0), statusCode,
        ""));
    when(mockResponse.getEntity()).thenReturn(returnedByteArray);

    return repository.retrieveModel(REVISION, "project_name", baseModelPath);
  }
}
