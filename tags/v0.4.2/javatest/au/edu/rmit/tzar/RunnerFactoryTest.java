package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.runners.NullRunner;
import junit.framework.TestCase;

import java.io.File;
import java.util.logging.Logger;

/**
 * Tests that the RunnerFactory correctly loads and instantiates Runner instances
 */
public class RunnerFactoryTest extends TestCase {
  public void testGetRunner_implicitPackage() throws TzarException {
    RunnerFactory runnerFactory = new RunnerFactory();
    Runner nullRunner = runnerFactory.getRunner("NullRunner");
    assertEquals(NullRunner.class, nullRunner.getClass());
  }

  public void testGetRunnerExplicitPackage() throws TzarException {
    RunnerFactory runnerFactory = new RunnerFactory();
    Runner nullRunner = runnerFactory.getRunner("au.edu.rmit.tzar.runners.NullRunner");
    assertEquals(NullRunner.class, nullRunner.getClass());
  }

  public void testGetRunnerNonStandardLocation() throws TzarException {
    RunnerFactory runnerFactory = new RunnerFactory();
    Runner runner = runnerFactory.getRunner("au.edu.rmit.tzar.TestRunner");
    assertEquals(TestRunner.class, runner.getClass());
    assertTrue(runner.runModel(null, null, null, null, null, null));
  }
}

class TestRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters, Logger logger) throws TzarException {
    return true;
  }
}
