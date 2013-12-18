package au.edu.rmit.tzar.repository;

import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;

/**
 * Tests for the HttpRepository
 */
public class HttpRepositoryTest extends BaseHttpRepositoryTest {
  @Override
  public void setUp() throws Exception {
    sourceUri = new URI("http://tzar-framework.googlecode.com/svn/trunk/java/version.properties?spec=svn201&r=193");
    baseModelPath = Files.createTempDir();
    repository = new HttpRepository(baseModelPath, sourceUri);
  }

  public void testRetrieveModel() throws Exception {
    File model = repository.retrieveModel(REVISION);
    assertTrue(model.exists());
    assertTrue(model.isFile());
    assertEquals(baseModelPath, model.getParentFile());
    BufferedReader reader = new BufferedReader(new FileReader(model));
    String line = reader.readLine();
    assertEquals("tzar 0.4.3", line);
  }
}
