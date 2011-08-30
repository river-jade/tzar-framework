package au.edu.rmit.tzar.repository;

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests for the LocalFileRepository class.
 */
public class LocalFileRepositoryTest extends TestCase {
  /**
   * Tests that getModel returns the provided path, independent of the provided revision number.
   */
  public void testGetModel() {
    File codePath = new File("/some/path");
    LocalFileRepository repository = new LocalFileRepository(codePath);
    assertEquals(codePath, repository.getModel("-1"));
    assertEquals(codePath, repository.getModel("909"));
    assertEquals(codePath, repository.getModel("head"));
  }
}
