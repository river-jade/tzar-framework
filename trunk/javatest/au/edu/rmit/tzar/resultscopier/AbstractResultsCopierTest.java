package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.runners.NullRunner;
import com.google.common.io.Files;
import junit.framework.TestCase;

import java.io.File;

/**
 * Base class for results copier tests.
 */
public abstract class AbstractResultsCopierTest extends TestCase {
  ResultsCopier copier;
  File baseDestDir;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    baseDestDir = Files.createTempDir();
  }

  /**
   * Creates a zero-length file in /tmp and attempts to copy it to another temp directory,
   * and checks that they are equal.
   *
   * @throws Exception if thrown, test has failed
   */
  public void testCopyResults() throws Exception {
    File tempSourceDir = Files.createTempDir();

    Run run = new Run(1234, "a run", "4321", "", Parameters.EMPTY_PARAMETERS, "scheduled", "");
    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tempSourceDir, null, new NullRunner());
    File localOutputPath = executableRun.getOutputPath();
    localOutputPath.mkdir();
    File sourcePath = File.createTempFile("file", null, localOutputPath);
    File sourcePath2 = File.createTempFile("file", null, localOutputPath);

    copier.copyResults(run, localOutputPath);

    File destDir = new File(baseDestDir, localOutputPath.getName());
    File destPath1 = new File(destDir, sourcePath.getName());
    File destPath2 = new File(destDir, sourcePath2.getName());
    assertTrue(Files.equal(sourcePath, destPath1));
    assertTrue(Files.equal(sourcePath2, destPath2));
  }
}
