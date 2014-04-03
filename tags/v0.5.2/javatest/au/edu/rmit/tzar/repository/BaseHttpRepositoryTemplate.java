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

    // I'm not really sure why we need to allow either of these, but it seems
    // as though when we're behind RMIT's proxy, we get VALIDATED, rather than
    // CACHE_HIT. This is acceptable, as it means we don't need to redownload the
    // entire file, but only check the server to ensure it hasn't changed, however,
    // it possibly just indicates that RMIT's proxy is misconfigured.
    assertTrue(CacheResponseStatus.CACHE_HIT.equals(cacheResponseStatus) ||
        CacheResponseStatus.VALIDATED.equals(cacheResponseStatus));
  }
}
