package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.repository.CodeRepository;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.python.google.common.collect.Maps;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the ExecutableRun class.
 */
public class ExecutableRunTest extends TestCase {
  public static final int RUN_ID = 1234;
  private static final String REVISION = "12334a";
  private static final File MODEL = new File("/path/to/model");
  private static final String PROJECT_NAME = "project name";
  private static final Map<String, String> VARIABLES = new HashMap<String, String>();
  { VARIABLES.put("aaa", "123");
    VARIABLES.put("aab", "124");
    VARIABLES.put("aac", "124$$run_id$$123");
  }
  private static final Map<String, String> INPUT_FILES = new HashMap<String, String>();
  private static final Map<String, String> OUTPUT_FILES = new HashMap<String, String>();
  private static final String RUNNER_CLASS = "MockRunner";
  private static final String SCENARIO_NAME = "a scenario";
  private static final String RUNSET = "runset_1";

  private Run run = mock(Run.class);
  private CodeRepository codeRepository = mock(CodeRepository.class);
  private RunnerFactory runnerFactory = mock(RunnerFactory.class);
  private ExecutableRun executableRun;
  private String OUTPUT_DIR;

  @Override
  public void setUp() throws Exception {
    File BASE_OUTPUT_PATH = Files.createTempDir();
    OUTPUT_DIR = new File(BASE_OUTPUT_PATH, Utils.Path.combineAndReplaceWhitespace("_", PROJECT_NAME, RUNSET,
        RUN_ID + "_" + SCENARIO_NAME)).toString();

    when(run.getRunId()).thenReturn(RUN_ID);
    when(run.getProjectName()).thenReturn(PROJECT_NAME);
    when(run.getScenarioName()).thenReturn(SCENARIO_NAME);
    when(run.getRunset()).thenReturn(RUNSET);
    when(run.getRunnerClass()).thenReturn(RUNNER_CLASS);
    when(run.getRevision()).thenReturn(REVISION);
    executableRun = ExecutableRun.createExecutableRun(run, BASE_OUTPUT_PATH, codeRepository,
        runnerFactory);
  }

  public void testCreateExecutableRun() {
    assertEquals(OUTPUT_DIR + ExecutableRun.INPROGRESS_SUFFIX, executableRun.getOutputPath().toString());
    assertEquals(RUN_ID, executableRun.getRunId());
    assertEquals(run, executableRun.getRun());

  }

  public void testExecuteSuccess() throws TzarException {
    testExecute(true);
    assertTrue(new File(OUTPUT_DIR).exists());
  }

  public void testExecuteFailure() throws TzarException {
    testExecute(false);
    assertTrue(new File(OUTPUT_DIR + ".failed").exists());
  }

  public void testExecute(boolean success) throws TzarException {
    when(codeRepository.getModel(REVISION)).thenReturn(MODEL);
    Parameters parameters = Parameters.createParameters(VARIABLES, INPUT_FILES, OUTPUT_FILES);
    when(run.getParameters()).thenReturn(parameters);
    when(runnerFactory.getRunner(RUNNER_CLASS)).thenReturn(new MockRunner(success));

    assertEquals(success, executableRun.execute());
  }

  public void testGetNextRunId() {
    // TBD
  }

  private class MockRunner implements Runner {
    private final boolean success;

    public MockRunner(boolean success) {
      this.success = success;
    }

    @Override
    public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
          Logger logger) throws TzarException {
      Map<String, String> variables = Maps.newHashMap(VARIABLES);
      variables.put("aac", "124" + RUN_ID + "123"); // because the id wildcard will be replaced
      assertEquals(Parameters.createParameters(variables, INPUT_FILES, OUTPUT_FILES), parameters);
      return success;
    }
  }
}
