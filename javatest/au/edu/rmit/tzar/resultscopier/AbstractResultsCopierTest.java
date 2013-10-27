package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import com.google.common.io.Files;
import junit.framework.TestCase;

import java.io.File;

/**
 * Base class for results copier tests.
 */
public abstract class AbstractResultsCopierTest extends TestCase {
  private static final String RUNNER_CLASS = "AClass";

  ResultsCopier copier;
  File baseDestPath;
  File sourcePath;
  File sourcePath2;
  protected File localOutputPath;
  protected Run run;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    File tempSourceDir = Files.createTempDir();

    run = new Run.Builder("a project", "a scenario")
        .build();

    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tempSourceDir, null, new RunnerFactory());
    localOutputPath = executableRun.getOutputPath();
    localOutputPath.mkdir();
    baseDestPath = Files.createTempDir();
    localOutputPath.mkdirs();
    sourcePath = File.createTempFile("file", null, localOutputPath);
    sourcePath2 = File.createTempFile("file", null, localOutputPath);
  }

}
