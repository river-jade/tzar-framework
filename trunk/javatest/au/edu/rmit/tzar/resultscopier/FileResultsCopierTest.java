package au.edu.rmit.tzar.resultscopier;

import com.google.common.io.Files;

import java.io.File;

/**
 * Tests that the local filesystem copier copies data as expected.
 */
public class FileResultsCopierTest extends AbstractResultsCopierTest {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    copier = new FileResultsCopier(baseDestPath);
  }

  /**
   * Creates a zero-length file in /tmp and attempts to copy it to another temp directory,
   * and checks that they are equal.
   *
   * @throws Exception if thrown, test has failed
   */
  public void testCopyResults() throws Exception {
    copier.copyResults(run, localOutputPath, true);

    File destDir = new File(baseDestPath, localOutputPath.getName());
    File destPath1 = new File(destDir, sourcePath.getName());
    File destPath2 = new File(destDir, sourcePath2.getName());
    assertTrue(Files.equal(sourcePath, destPath1));
    assertTrue(Files.equal(sourcePath2, destPath2));
  }
}
