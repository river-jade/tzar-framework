package au.edu.rmit.tzar.repository;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Tests for the LocalFileRepository class.
 */
public class LocalFileRepositoryTest extends TestCase {
  /**
   * Tests that retrieveModel returns the provided path, independent of the provided revision number.
   */
  public void testGetModel() throws URISyntaxException {
    URI codePathStr = new URI("file", null, "/some/path", null);
    File codePath = new File(codePathStr);
    LocalFileRepository repository = new LocalFileRepository(codePathStr);
    assertEquals(codePath, repository.retrieveModel("-1"));
    assertEquals(codePath, repository.retrieveModel("909"));
    assertEquals(codePath, repository.retrieveModel("head"));
  }
}
