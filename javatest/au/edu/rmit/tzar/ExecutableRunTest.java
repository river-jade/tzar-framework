package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.beans.DownloadMode;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.ArgumentCaptor;
import org.python.google.common.collect.Maps;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * Tests for the ExecutableRun class.
 */
public class ExecutableRunTest extends TestCase {
  public static final int RUN_ID = 1234;
  private static final String REVISION = "12334a";
  private static final File MODEL = new File("/path/to/model");
  private static final String PROJECT_NAME = "project name";
  private static final String RUNNER_CLASS = "MockRunner";
  private static final String SCENARIO_NAME = "a scenario";
  private static final String RUNSET = "runset_1";

  private static final File SOURCE_PATH = new File("/path/to/code");
  private static final String RUNNER_FLAGS = "--aflag=avalue";
  private Run run;

  private Map<String, Object> variables = Maps.newHashMap();
  private File outputDir;
  private RunnerFactory runnerFactory = mock(RunnerFactory.class);
  private Map<String, CodeSourceImpl> libraries = ImmutableMap.of();
  private Runner mockRunner = mock(Runner.class);
  private Parameters parameters;
  private File tzarOutputPath;
  private CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);

  @Override
  public void setUp() throws Exception {
    tzarOutputPath = Files.createTempDir();
    outputDir = new File(tzarOutputPath, PathUtils.combineAndReplaceWhitespace("_", PROJECT_NAME, RUNSET,
        RUN_ID + "_" + SCENARIO_NAME));

    variables.put("aaa", "123");
    variables.put("aab", "124");
    variables.put("aac", "124$$run_id$$123");
  }

  private ExecutableRun createExecutableRun(File TZAR_OUTPUT_PATH) throws TzarException {
    parameters = Parameters.createParameters(variables);
    CodeSourceImpl modelSource = new CodeSourceImpl(mockHttpClient, SOURCE_PATH.toURI(),
        CodeSourceImpl.RepositoryTypeImpl.LOCAL_FILE, REVISION, DownloadMode.FORCE);

    Run.ProjectInfo projectInfo = new Run.ProjectInfo(PROJECT_NAME, modelSource, libraries, RUNNER_CLASS, RUNNER_FLAGS);
    run = new Run(projectInfo, SCENARIO_NAME)
        .setRunset(RUNSET)
        .setRunId(RUN_ID)
        .setParameters(parameters);
    return ExecutableRun.createExecutableRun(run, TZAR_OUTPUT_PATH, MODEL, runnerFactory);
  }

  public void testCreateExecutableRun() throws TzarException {
    ExecutableRun executableRun = createExecutableRun(tzarOutputPath);
    assertEquals(outputDir + Constants.INPROGRESS_SUFFIX, executableRun.getOutputPath().toString());
    assertEquals(RUN_ID, executableRun.getRunId());
    assertEquals(run, executableRun.getRun());
  }

  public void testExecuteSuccess() throws TzarException {
    testExecute(true);
    assertTrue(outputDir.exists());
  }

  public void testExecuteFailure() throws TzarException {
    testExecute(false);
    assertTrue(new File(outputDir + ".failed").exists());
  }

  public void testGetNextRunId() {
    // TBD
  }

  public void testLibraryParamReplacement() throws Exception {
    libraries = ImmutableMap.of(
        "lib1", new CodeSourceImpl(mockHttpClient, new URI("file:///source/code/1"),
            CodeSourceImpl.RepositoryTypeImpl.LOCAL_FILE, "123", DownloadMode.FORCE),
        "lib2", new CodeSourceImpl(mockHttpClient, new URI("file:///source/code/2"),
            CodeSourceImpl.RepositoryTypeImpl.LOCAL_FILE, "223", DownloadMode.FORCE),
        "lib3", new CodeSourceImpl(mockHttpClient, new URI("file:///source/code/3"),
            CodeSourceImpl.RepositoryTypeImpl.LOCAL_FILE, "323", DownloadMode.CACHE));

    // we rerun setup to recreate the objects that depend on library. a bit dodgy though.
    setUp();
    variables.put("libtest", "$$library_path(lib1)$$/2");
    createExecutableRun(tzarOutputPath);

    Map<String, Object> changedVariables = Maps.newHashMap(parameters.asMap());
    changedVariables.put("aac", "124" + RUN_ID + "123"); // because the id wildcard will be replaced
    changedVariables.put("libtest", new File("/source/code/1") + File.separator + "2");

    testExecute(true, changedVariables);
    assertTrue(outputDir.exists());
  }

  private void testExecute(boolean success) throws TzarException {
    Map<String, Object> expectedVariables = Maps.newHashMap();
    expectedVariables.put("aac", "124" + RUN_ID + "123"); // because the id wildcard will be replaced
    testExecute(success, expectedVariables);
  }

  private void testExecute(boolean success, Map<String, Object> expectedVariables) throws TzarException {
    ExecutableRun executableRun = createExecutableRun(tzarOutputPath);

    when(runnerFactory.getRunner(RUNNER_CLASS)).thenReturn(mockRunner);
    when(mockRunner.runModel(any(File.class), any(File.class), anyString(), anyString(), any(Parameters.class),
        any(Logger.class), any(StopRun.class))).thenReturn(success);
    boolean result = executableRun.execute(new StopRun());

    ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    verify(mockRunner).runModel(eq(SOURCE_PATH.getAbsoluteFile()),
        eq(new File(outputDir.toString() + Constants.INPROGRESS_SUFFIX)),
        eq(Integer.toString(RUN_ID)), eq(RUNNER_FLAGS),
        parametersArgumentCaptor.capture(), isA(Logger.class), isA(StopRun.class));

    Parameters expected = parameters.mergeParameters(Parameters.createParameters(expectedVariables));

    assertEquals(expected, parametersArgumentCaptor.getValue());
    assertEquals(success, result);
  }
}
