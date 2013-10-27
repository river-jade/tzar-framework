package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.repository.CodeSource;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.mockito.InOrder;
import org.postgresql.jdbc4.Jdbc4Connection;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests for the RunDao.
 */
public class RunDaoTest extends TestCase {
  private static final int RUN_ID = 1234;
  private static final String CODE_VERSION = "4321";
  private static final String PROJECT_NAME = "cool project";
  private static final String SCENARIO_NAME = "cool scenario";
  private static final String COMMAND_FLAGS = "-pexample";
  private static final String RUNSET = "a runset";
  private static final String CLUSTER_NAME = "a cluster";
  private static final String RUNNER_CLASS = "AClass";

  public static final int FIRST_RUN_ID = 2233;
  private static final String REPO_TYPE = "SVN";
  private static final String MODEL_URL = "http://some.subversion/repo";

  private Jdbc4Connection mockConnection;
  private PreparedStatement insertRun;
  private ResultSet resultSet;
  private RunDao runDao;
  private CodeSource codeSource;

  public void setUp() throws Exception {
    mockConnection = mock(Jdbc4Connection.class);
    insertRun = mock(PreparedStatement.class);
    PreparedStatement nextRunStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    ParametersDao mockParametersDao = mock(ParametersDao.class);
    codeSource = new CodeSource(new URI(MODEL_URL), CodeSource.RepositoryType.SVN, CODE_VERSION);

    ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
    when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);

    when(resultSet.getInt("run_id")).thenReturn(RUN_ID);
    when(resultSet.getString("project_name")).thenReturn(PROJECT_NAME);
    when(resultSet.getString("scenario_name")).thenReturn(SCENARIO_NAME);
    when(resultSet.getString("runner_flags")).thenReturn(COMMAND_FLAGS);
    when(resultSet.getString("runset")).thenReturn(RUNSET);
    when(resultSet.getString("cluster_name")).thenReturn(CLUSTER_NAME);
    when(resultSet.getString("model_repo_type")).thenReturn(REPO_TYPE);
    when(resultSet.getString("model_url")).thenReturn(MODEL_URL);
    when(resultSet.getString("model_revision")).thenReturn(CODE_VERSION);
    when(resultSet.getString("state")).thenReturn("scheduled");
    when(resultSet.getString("runner_class")).thenReturn(RUNNER_CLASS);
    when(mockConnection.prepareStatement(RunDao.NEXT_RUN_SQL, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY)).thenReturn(nextRunStatement);
    when(mockConnection.prepareStatement(RunDao.INSERT_RUN_SQL)).thenReturn(insertRun);
    when(nextRunStatement.executeQuery()).thenReturn(resultSet);
    when(mockParametersDao.loadFromDatabase(RUN_ID)).thenReturn(Parameters.EMPTY_PARAMETERS);
    runDao = new RunDao(mockConnectionFactory, mockParametersDao);
  }

  public void testGetNextRun() throws Exception {
    when(resultSet.next()).thenReturn(true);
    Run run = new Run(PROJECT_NAME, SCENARIO_NAME, codeSource)
        .setRunId(RUN_ID)
        .setRunnerFlags(COMMAND_FLAGS)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setClusterName(CLUSTER_NAME)
        .setRunnerClass(RUNNER_CLASS);
    assertEquals(run, runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testGetNextRunNoMatch() throws Exception {
    when(resultSet.next()).thenReturn(false);
    assertNull(runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testInsertRuns() throws TzarException, SQLException, URISyntaxException {
    List<Run> runs = Lists.newArrayList();

    Run run = new Run(PROJECT_NAME, SCENARIO_NAME, codeSource)
        .setRunId(RUN_ID)
        .setRunnerFlags(COMMAND_FLAGS)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setState("state")
        .setClusterName(CLUSTER_NAME)
        .setRunnerClass(RUNNER_CLASS);

    runs.add(run);

    CodeSource codeSource2 = new CodeSource(new URI(MODEL_URL), CodeSource.RepositoryType.LOCAL_FILE, CODE_VERSION + 1);
    run = new Run(PROJECT_NAME, SCENARIO_NAME + 1, codeSource2)
        .setRunId(RUN_ID)
        .setRunnerFlags(COMMAND_FLAGS)
        .setParameters(Parameters.EMPTY_PARAMETERS)
        .setRunset(RUNSET)
        .setState("state")
        .setClusterName(CLUSTER_NAME)
        .setRunnerClass(RUNNER_CLASS);
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
    inOrder.verify(insertRun).setString(8, COMMAND_FLAGS);
    inOrder.verify(insertRun).setString(9, RUNSET);
    inOrder.verify(insertRun).setString(10, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(11, RUNNER_CLASS);
    inOrder.verify(insertRun).setInt(1, FIRST_RUN_ID + 1);
    inOrder.verify(insertRun).setString(2, "scheduled");
    inOrder.verify(insertRun).setString(3, MODEL_URL);
    inOrder.verify(insertRun).setString(4, CodeSource.RepositoryType.LOCAL_FILE.name());
    inOrder.verify(insertRun).setString(5, CODE_VERSION + 1);
    inOrder.verify(insertRun).setString(6, PROJECT_NAME);
    inOrder.verify(insertRun).setString(7, SCENARIO_NAME + "1");
    inOrder.verify(insertRun).setString(8, COMMAND_FLAGS);
    inOrder.verify(insertRun).setString(9, RUNSET);
    inOrder.verify(insertRun).setString(10, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(11, RUNNER_CLASS);
  }
}
