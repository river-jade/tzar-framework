package au.edu.rmit.tzar.repository;

import com.google.common.io.Files;
import junit.framework.TestCase;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;

/**
 * Tests for the HttpRepository
 */
public class HttpRepositoryTest extends TestCase {
  private HttpRepository httpRepository;
  private URI sourceUri;

  @Override
  public void setUp() throws Exception {
    sourceUri = new URI("http://tzar-framework.googlecode.com/svn/trunk/java/version.properties?spec=svn201&r=193");
    httpRepository = new HttpRepository(Files.createTempDir(), sourceUri);
  }

  public void testRetrieveModel() throws Exception {
    File model = httpRepository.retrieveModel("");
    assertTrue(model.exists());
    assertTrue(model.isFile());
    BufferedReader reader = new BufferedReader(new FileReader(model));
    String line = reader.readLine();
    assertEquals("tzar 0.4.3", line);
  }

  public void testCaching() throws Exception {
    HttpCacheContext cacheContext = new HttpCacheContext();
    httpRepository.client.execute(new HttpGet(sourceUri), cacheContext);
    CacheResponseStatus cacheResponseStatus = cacheContext.getCacheResponseStatus();
    assertEquals(CacheResponseStatus.CACHE_MISS, cacheResponseStatus);
    httpRepository.client.execute(new HttpGet(sourceUri), cacheContext);
    cacheResponseStatus = cacheContext.getCacheResponseStatus();
    assertEquals(CacheResponseStatus.CACHE_HIT, cacheResponseStatus);
  }
}
