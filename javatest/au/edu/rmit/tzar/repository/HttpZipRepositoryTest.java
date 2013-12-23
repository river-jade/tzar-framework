package au.edu.rmit.tzar.repository;

import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;

/**
 * Tests for the HttpRepository
 */
public class HttpZipRepositoryTest extends BaseHttpRepositoryTemplate {
  @Override
  public void setUp() throws Exception {
    sourceUri = new URI("http://tzar-framework.googlecode.com/svn-history/r206/trunk/javatest/test.zip");
    baseModelPath = Files.createTempDir();
    repository = new HttpZipRepository(baseModelPath, sourceUri);
  }

  public void testRetrieveModel() throws Exception {
    File model = repository.retrieveModel(REVISION, "module_name");
    assertTrue(model.exists());
    assertTrue(model.isDirectory());
    assertEquals(baseModelPath, model.getParentFile());
    File[] files = model.listFiles();
    assertEquals(2, files.length);

    for (File file : files) {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line = reader.readLine();
      assertEquals("Tzar FTW!", line);
    }
  }

}
