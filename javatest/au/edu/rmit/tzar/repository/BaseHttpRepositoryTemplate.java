package au.edu.rmit.tzar.repository;

import junit.framework.TestCase;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.net.URI;

/**
 * Base class for Http and HttpZip repository tests.
 */
public abstract class BaseHttpRepositoryTemplate extends TestCase {
  public static final String REVISION = "";
  protected HttpRepository repository;
  protected URI sourceUri;
  protected File baseModelPath;

  public void testCaching() throws Exception {
    HttpCacheContext cacheContext = new HttpCacheContext();
    repository.client.execute(new HttpGet(sourceUri), cacheContext);
    CacheResponseStatus cacheResponseStatus = cacheContext.getCacheResponseStatus();
    assertEquals(CacheResponseStatus.CACHE_MISS, cacheResponseStatus);
    repository.client.execute(new HttpGet(sourceUri), cacheContext);
    cacheResponseStatus = cacheContext.getCacheResponseStatus();
    assertEquals(CacheResponseStatus.CACHE_HIT, cacheResponseStatus);
  }
}
