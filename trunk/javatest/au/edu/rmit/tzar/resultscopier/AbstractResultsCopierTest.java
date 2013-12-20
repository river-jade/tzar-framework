package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.runners.RunnerFactory;
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
    File tzarOutputPath = Files.createTempDir();

    run = new Run(new Run.ProjectInfo("a project", null, null, null, null), "a scenario");

    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tzarOutputPath, tempSourceDir,
        new RunnerFactory());
    localOutputPath = executableRun.getOutputPath();
    localOutputPath.mkdir();
    baseDestPath = Files.createTempDir();
    localOutputPath.mkdirs();
    sourcePath = File.createTempFile("file", null, localOutputPath);
    sourcePath2 = File.createTempFile("file", null, localOutputPath);
  }

}
