package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import junit.framework.Assert;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Tests for the HttpRepository
 */
public class HttpRepositoryTest extends BaseHttpRepositoryTemplate {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    repository = new HttpRepository(mockHttpClient, sourceUri, false);
    returnedByteArray = new ByteArrayEntity(EXPECTED.getBytes());
  }

  public void testRetrieveModelSuccess() throws Exception {
    File model = retrieveModel(HttpStatus.SC_OK);
    assertTrue(model.exists());
    assertTrue(model.isFile());
    assertEquals(baseModelPath, model.getParentFile());
    BufferedReader reader = new BufferedReader(new FileReader(model));
    String line = reader.readLine();
    Assert.assertEquals(HttpRepositoryTest.EXPECTED, line);
  }

  public void testRetrieveModel500() throws IOException {
    try {
      retrieveModel(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      fail("Exception was not thrown.");
    } catch (TzarException e) {
      // expected.
    }
  }

}
