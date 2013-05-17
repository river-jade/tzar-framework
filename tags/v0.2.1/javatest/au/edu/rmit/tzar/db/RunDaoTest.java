package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.mockito.InOrder;
import org.postgresql.jdbc4.Jdbc4Connection;

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
  private static final String RUN_NAME = "cool run";
  private static final String COMMAND_FLAGS = "-pexample";
  private static final String RUNSET = "a runset";
  private static final String CLUSTER_NAME = "a cluster";
  private static final String RUNNER_CLASS = "AClass";

  public static final int FIRST_RUN_ID = 2233;

  private Jdbc4Connection mockConnection;
  private PreparedStatement insertRun;
  private ResultSet resultSet;
  private RunDao runDao;

  public void setUp() throws Exception {
    mockConnection = mock(Jdbc4Connection.class);
    PreparedStatement nextRunStatement = mock(PreparedStatement.class);
    insertRun = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    ParametersDao mockParametersDao = mock(ParametersDao.class);

    when(resultSet.getInt("run_id")).thenReturn(RUN_ID);
    when(resultSet.getString("run_name")).thenReturn(RUN_NAME);
    when(resultSet.getString("command_flags")).thenReturn(COMMAND_FLAGS);
    when(resultSet.getString("runset")).thenReturn(RUNSET);
    when(resultSet.getString("cluster_name")).thenReturn(CLUSTER_NAME);
    when(resultSet.getString("code_version")).thenReturn(CODE_VERSION);
    when(resultSet.getString("state")).thenReturn("scheduled");
    when(resultSet.getString("runner_class")).thenReturn(RUNNER_CLASS);
    when(mockConnection.prepareStatement(RunDao.NEXT_RUN_SQL, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY)).thenReturn(nextRunStatement);
    when(mockConnection.prepareStatement(RunDao.INSERT_RUN_SQL)).thenReturn(insertRun);
    when(nextRunStatement.executeQuery()).thenReturn(resultSet);
    when(mockParametersDao.loadFromDatabase(RUN_ID)).thenReturn(Parameters.EMPTY_PARAMETERS);
    runDao = new RunDao(mockConnection, mockParametersDao);
  }

  public void testGetNextRun() throws Exception {
    when(resultSet.next()).thenReturn(true);
    assertEquals(new Run(RUN_ID, RUN_NAME, CODE_VERSION, COMMAND_FLAGS, Parameters.EMPTY_PARAMETERS,
        "scheduled", RUNSET, CLUSTER_NAME, RUNNER_CLASS), runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testGetNextRunNoMatch() throws Exception {
    when(resultSet.next()).thenReturn(false);
    assertNull(runDao.getNextRun(null, CLUSTER_NAME));
  }

  public void testInsertRuns() throws RdvException, SQLException {
    ResultSet generatedKeys = mock(ResultSet.class);

    when(mockConnection.execSQLQuery("select nextval('runs_run_id_seq')")).thenReturn(generatedKeys);
    when(generatedKeys.getInt(1)).thenReturn(FIRST_RUN_ID);

    List<Run> runs = Lists.newArrayList();
    runs.add(new Run(RUN_ID, RUN_NAME, CODE_VERSION, COMMAND_FLAGS,
        Parameters.EMPTY_PARAMETERS, "state", RUNSET, CLUSTER_NAME, RUNNER_CLASS));
    runs.add(new Run(RUN_ID, RUN_NAME + "1", CODE_VERSION + 1, COMMAND_FLAGS,
        Parameters.EMPTY_PARAMETERS, "state", RUNSET, CLUSTER_NAME, RUNNER_CLASS));

    InOrder inOrder = inOrder(insertRun, mockConnection);
    runDao.insertRuns(runs);
    inOrder.verify(insertRun).setInt(1, FIRST_RUN_ID);
    inOrder.verify(insertRun).setString(2, "scheduled");
    inOrder.verify(insertRun).setString(3, CODE_VERSION);
    inOrder.verify(insertRun).setString(4, RUN_NAME);
    inOrder.verify(insertRun).setString(5, COMMAND_FLAGS);
    inOrder.verify(insertRun).setString(6, RUNSET);
    inOrder.verify(insertRun).setString(7, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(8, RUNNER_CLASS);
    inOrder.verify(insertRun).setInt(1, FIRST_RUN_ID + 1);
    inOrder.verify(insertRun).setString(2, "scheduled");
    inOrder.verify(insertRun).setString(3, CODE_VERSION + 1);
    inOrder.verify(insertRun).setString(4, RUN_NAME + "1");
    inOrder.verify(insertRun).setString(5, COMMAND_FLAGS);
    inOrder.verify(insertRun).setString(6, RUNSET);
    inOrder.verify(insertRun).setString(7, CLUSTER_NAME);
    inOrder.verify(insertRun).setString(8, RUNNER_CLASS);
    inOrder.verify(mockConnection).setAutoCommit(true);
  }
}