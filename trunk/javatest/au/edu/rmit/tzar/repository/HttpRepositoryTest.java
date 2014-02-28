package au.edu.rmit.tzar.repository;

import com.google.common.io.Files;

import java.net.URI;

/**
 * Tests for the HttpRepository
 */
public class HttpRepositoryTest extends BaseHttpRepositoryTemplate {
  @Override
  public void setUp() throws Exception {
    sourceUri = new URI("http://tzar-framework.googlecode.com/svn/trunk/java/version.properties?spec=svn201&r=193");
    baseModelPath = Files.createTempDir();
    repository = new HttpRepository(baseModelPath, sourceUri, true);
  }

  // TODO(river): this test is disabled because it fails in certain circumstances
  // when behind a proxy server. I haven't been able to determine what is causing
  // this to occur, so disabling the test for now.
//  public void testRetrieveModel() throws Exception {
//    File model = repository.retrieveModel(REVISION, "project_name");
//    assertTrue(model.exists());
//    assertTrue(model.isFile());
//    assertEquals(baseModelPath, model.getParentFile());
//    BufferedReader reader = new BufferedReader(new FileReader(model));
//    String line = reader.readLine();
//    assertEquals("tzar 0.4.3", line);
//  }
}
