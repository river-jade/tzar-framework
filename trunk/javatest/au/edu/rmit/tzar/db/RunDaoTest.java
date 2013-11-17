package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.CodeSource;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.mockito.InOrder;
import org.postgresql.jdbc4.Jdbc4Connection;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests for the RunDao.
 */
// TODO(river): add tests for libraries
public class RunDaoTest extends TestCase {
  private static final int RUN_ID = 1234;
  private static final String CODE_VERSION = "4321";
  private static final String PROJECT_NAME = "cool project";
  private static final String SCENARIO_NAME = "cool scenario";
  private static final String RUNNER_FLAGS = "-pexample";
  private static final String RUNSET = "a runset";
  private static final String CLUSTER_NAME = "a cluster";
  private static final String RUNNER_CLASS = "AClass";

  public static final int FIRST_RUN_ID = 2233;
  private static final String REPO_TYPE = "SVN";
  private static final String MODEL_URL = "http://some.subversion/repo";

  private Jdbc4Connection mockConnection;
  private PreparedStatement insertRun;
  private PreparedStatement updateRun;
  private ResultSet resultSet;
  private RunDao runDao;
  private Run.ProjectInfo projectInfo;
  private ParametersDao.BatchInserter mockBatchInserter;
  private LibraryDao mockLibraryDao;
  private ParametersDao mockParametersDao;

  public void setUp() throws Exception {
    mockConnection = mock(Jdbc4Connection.class);
    insertRun = mock(PreparedStatement.class);
    updateRun = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    mockParametersDao = mock(ParametersDao.class);
    mockLibraryDao = mock(LibraryDao.class);
    mockBatchInserter = mock(ParametersDao.BatchInserter.class);

    ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
    when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);

    when(mockConnection.prepareStatement(isA(String.class))).thenReturn(mock(PreparedStatement.class));
    when(mockParametersDao.createBatchInserter(mockConnection)).thenReturn(mockBatchInserter);
    runDao = new RunDao(mockConnectionFactory, mockParametersDao, mockLibraryDao);

    CodeSourceImpl codeSource = new CodeSourceImpl(new URI(MODEL_URL), CodeSourceImpl.RepositoryType.SVN, CODE_VERSION);
    ImmutableMap<String, CodeSource> library = ImmutableMap.of();
    projectInfo = new Run.ProjectInfo(PROJECT_NAME, codeSource, library, RUNNER_CLASS, RUNNER_FLAGS);
  }

  public void testGetNextRun() throws Exception {
    setupResultSet();
    PreparedStatement nextRunStatement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(RunDao.NEXT_RUN_SQL)).thenReturn(nextRunStatement);
    when(nextRunStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(mockLibraryDao.getLibraries(RUN_ID, mockConnection)).thenReturn(ImmutableMap.<String, CodeSource>of());
    when(mockParametersDao.loadFromDatabase(RUN_ID, mockConnection)).thenReturn(Parameters.EMPTY_PARAMETERS);
    Run run = new Run(projectInfo, SCENARIO_NAME)
        .setRunId(RUN_ID)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setClusterName(CLUSTER_NAME);
    assertEquals(run, runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testGetNextRunNoMatch() throws Exception {
    PreparedStatement nextRunStatement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(RunDao.NEXT_RUN_SQL)).thenReturn(nextRunStatement);
    when(nextRunStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);
    assertNull(runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testInsertRuns() throws TzarException, SQLException, URISyntaxException {
    when(mockConnection.prepareStatement(RunDao.INSERT_RUN_SQL)).thenReturn(insertRun);
    List<Run> runs = Lists.newArrayList();

    Run run = new Run(projectInfo, SCENARIO_NAME)
        .setRunId(RUN_ID)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setClusterName(CLUSTER_NAME);

    runs.add(run);

    CodeSourceImpl codeSource2 = new CodeSourceImpl(new URI(MODEL_URL), CodeSourceImpl.RepositoryType.LOCAL_FILE,
        CODE_VERSION + 1);
    Run.ProjectInfo projectInfo2 = new Run.ProjectInfo(PROJECT_NAME, codeSource2, null, RUNNER_CLASS, RUNNER_FLAGS);
    run = new Run(projectInfo2, SCENARIO_NAME + 1)
        .setRunId(RUN_ID)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setState(Run.State.IN_PROGRESS)
        .setClusterName(CLUSTER_NAME);
    runs.add(run);

    ResultSet generatedKeys = mock(ResultSet.class);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement("select nextval('runs_run_id_seq')")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(generatedKeys);
    when(generatedKeys.getInt(1)).thenReturn(FIRST_RUN_ID);

    when(mockConnection.prepareStatement("select setval('runs_run_id_seq', " + (FIRST_RUN_ID + runs.size()) +
        ", false)")).thenReturn(statement);

    when(mockConnection.prepareStatement(ParametersDao.INSERT_PARAM_SQL)).thenReturn(statement);

    InOrder inOrder = inOrder(insertRun, mockConnection);
    runDao.insertRuns(runs);
    inOrder.verify(insertRun).setInt(1, FIRST_RUN_ID);
    inOrder.verify(insertRun).setString(2, "scheduled");
    inOrder.verify(insertRun).setString(3, MODEL_URL);
    inOrder.verify(insertRun).setString(4, REPO_TYPE);
    inOrder.verify(insertRun).setString(5, CODE_VERSION);
    inOrder.verify(insertRun).setString(6, PROJECT_NAME);
    inOrder.verify(insertRun).setString(7, SCENARIO_NAME);
    inOrder.verify(insertRun).setString(8, RUNNER_FLAGS);
    inOrder.verify(insertRun).setString(9, RUNSET);
    inOrder.verify(insertRun).setString(10, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(11, RUNNER_CLASS);
    inOrder.verify(insertRun).setInt(1, FIRST_RUN_ID + 1);
    inOrder.verify(insertRun).setString(2, "in_progress");
    inOrder.verify(insertRun).setString(3, MODEL_URL);
    inOrder.verify(insertRun).setString(4, CodeSourceImpl.RepositoryType.LOCAL_FILE.name());
    inOrder.verify(insertRun).setString(5, CODE_VERSION + 1);
    inOrder.verify(insertRun).setString(6, PROJECT_NAME);
    inOrder.verify(insertRun).setString(7, SCENARIO_NAME + "1");
    inOrder.verify(insertRun).setString(8, RUNNER_FLAGS);
    inOrder.verify(insertRun).setString(9, RUNSET);
    inOrder.verify(insertRun).setString(10, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(11, RUNNER_CLASS);
  }

  public void testInsertRunWithParams() throws Exception {
    setupResultSet();
    List<Run> runs = Lists.newArrayList();

    Parameters parameters = Parameters.createParameters(
        ImmutableMap.<String, String>of("var1", "varval1"),
        ImmutableMap.<String, String>of("in1", "inval1"),
        ImmutableMap.<String, String>of("out1", "outval1")
    );
    Run run = new Run(projectInfo, SCENARIO_NAME)
        .setRunId(RUN_ID)
        .setParameters(parameters)
        .setRunset(RUNSET)
        .setClusterName(CLUSTER_NAME);

    runs.add(run);

    ResultSet generatedKeys = mock(ResultSet.class);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement("select nextval('runs_run_id_seq')")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(generatedKeys);
    when(generatedKeys.getInt(1)).thenReturn(FIRST_RUN_ID);

    when(mockConnection.prepareStatement("select setval('runs_run_id_seq', " + (FIRST_RUN_ID + runs.size()) +
        ", false)")).thenReturn(statement);

    when(mockConnection.prepareStatement(ParametersDao.INSERT_PARAM_SQL)).thenReturn(statement);

    runDao.insertRuns(runs);

    verify(mockBatchInserter, times(1)).insertParams(FIRST_RUN_ID, parameters);
    verify(mockBatchInserter, times(1)).executeBatch();
  }

  public void testUpdateRun() throws TzarException, SQLException {
    when(mockConnection.prepareStatement(RunDao.UPDATE_RUN_SQL)).thenReturn(updateRun);

    Date START_TIME = new GregorianCalendar(2012, 12, 25, 12, 15).getTime();
    Date END_TIME = new GregorianCalendar(2012, 12, 31, 11, 59).getTime();
    String HOSTNAME = "foo.bar.com";
    String HOST_IP = "123.213.111.222";
    File OUTPUT_PATH = new File("/bar/baz/spiffy");
    String OUTPUT_HOST = "output.bar.com";
    Run.State STATE = Run.State.COPIED;
    Run run = new Run(projectInfo, SCENARIO_NAME)
        .setRunId(RUN_ID)
        .setStartTime(START_TIME)
        .setEndTime(END_TIME)
        .setState(STATE)
        .setHostname(HOSTNAME)
        .setHostIp(HOST_IP)
        .setRemoteOutputPath(OUTPUT_PATH)
        .setOutputHost(OUTPUT_HOST);

    InOrder inOrder = inOrder(updateRun, mockConnection);
    runDao.persistRun(run);
    inOrder.verify(updateRun).setTimestamp(1, new java.sql.Timestamp(START_TIME.getTime()), RunDao.UTC);
    inOrder.verify(updateRun).setTimestamp(2, new java.sql.Timestamp(END_TIME.getTime()), RunDao.UTC);
    inOrder.verify(updateRun).setString(3, STATE.name().toLowerCase());
    inOrder.verify(updateRun).setString(4, HOSTNAME);
    inOrder.verify(updateRun).setString(5, HOST_IP);
    inOrder.verify(updateRun).setString(6, OUTPUT_PATH.getAbsolutePath());
    inOrder.verify(updateRun).setString(7, OUTPUT_HOST);
    inOrder.verify(updateRun).setInt(8, RUN_ID);
    inOrder.verify(updateRun).executeUpdate();
    inOrder.verify(mockConnection).commit();
  }

  private void setupResultSet() throws SQLException {
    when(resultSet.getInt("run_id")).thenReturn(RUN_ID);
    when(resultSet.getString("project_name")).thenReturn(PROJECT_NAME);
    when(resultSet.getString("scenario_name")).thenReturn(SCENARIO_NAME);
    when(resultSet.getString("runner_flags")).thenReturn(RUNNER_FLAGS);
    when(resultSet.getString("runset")).thenReturn(RUNSET);
    when(resultSet.getString("cluster_name")).thenReturn(CLUSTER_NAME);
    when(resultSet.getString("model_repo_type")).thenReturn(REPO_TYPE);
    when(resultSet.getString("model_url")).thenReturn(MODEL_URL);
    when(resultSet.getString("model_revision")).thenReturn(CODE_VERSION);
    when(resultSet.getString("state")).thenReturn("scheduled");
    when(resultSet.getString("runner_class")).thenReturn(RUNNER_CLASS);
  }
}
