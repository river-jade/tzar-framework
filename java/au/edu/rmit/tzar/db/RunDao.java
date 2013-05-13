package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.jcabi.aspects.RetryOnFailure;

import java.io.File;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle Run management, including loading and persisting to the database.
 */
public class RunDao {
  private static final Logger LOG = Logger.getLogger(RunDao.class.getName());

  @VisibleForTesting
  static final String INSERT_RUN_SQL = "INSERT INTO runs (run_id, state, code_version, project_name, scenario_name, " +
      "command_flags, runset, cluster_name, runner_class) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  @VisibleForTesting
  static final String NEXT_RUN_SQL = "SELECT run_id, state, code_version, project_name, scenario_name, command_flags, " +
      "runset, cluster_name, output_path, output_host, runner_class FROM runs WHERE state='scheduled' AND " +
      "runset LIKE ? AND cluster_name = ? ORDER BY run_id ASC LIMIT 1";

  @VisibleForTesting
  static final String UPDATE_RUN_SQL = "UPDATE runs SET run_start_time = ?, run_end_time = ?, state = ?, " +
      "hostname = ?, output_path = ?, output_host = ?, runner_class = ? where run_id = ?";

  // select for update locks the row in question for modification, so that we can guarantee
  // than another node does not write to the row before we do
  @VisibleForTesting
  static final String SELECT_RUN_SQL = "SELECT state FROM runs where run_id = ? FOR UPDATE";

  private final ParametersDao parametersDao;
  private final ConnectionFactory connectionFactory;

  public RunDao(ConnectionFactory connectionFactory, ParametersDao parametersDao) throws RdvException {
      this.connectionFactory = connectionFactory;
      this.parametersDao = parametersDao;
  }

  /**
   * Polls the database for the next scheduled run. If a scheduled run is found, the run is marked as
   * 'in_progress' and persisted to the database. This is done in a single transaction, to avoid two nodes
   * from executing the same run.
   *
   * @param runset runset to filter by or null to poll for any runset
   * @param clusterName we only poll for runs scheduled for the current cluster. Not null, but may be empty.
   * @return true if a run was found, false otherwise
   * @throws RdvException if something goes wrong executing the run
   */
  public synchronized Run getNextRun(String runset, String clusterName) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      connection.setAutoCommit(false);
      PreparedStatement selectNextRun = connection.prepareStatement(NEXT_RUN_SQL, ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      selectNextRun.setString(1, runset == null ? "%" : runset);
      selectNextRun.setString(2, clusterName);
      ResultSet resultSet = selectNextRun.executeQuery();

      Run run;
      if (resultSet.next()) {
        run = runFromResultSet(resultSet, true);
      } else {
        run = null;
      }
      exceptionOccurred = false;
      return run;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  public boolean markRunInProgress(Run run) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;

    try {
      connection.setAutoCommit(false);
      PreparedStatement selectRun = connection.prepareStatement(SELECT_RUN_SQL);
      selectRun.setInt(1, run.getRunId());
      ResultSet resultSet = selectRun.executeQuery();
      if (!resultSet.next()) {
        throw new RdvException("Expected to find record matching run_id: " + run.getRunId());
      }
      String status = resultSet.getString("state");
      if (!"scheduled".equals(status)) {
        LOG.fine("Expected run to have status 'scheduled', but status was: " + status + ". This probably indicates" +
            " that another node has grabbed the run.");
        return false;
      }
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      throw new RdvException(e);
    }

    persistRun(run, connection);
    return true;
  }

  public synchronized void persistRun(Run run) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    persistRun(run, connection);
  }

  @RetryOnFailure(attempts = 5, delay=10, unit=TimeUnit.SECONDS)
  private void persistRun(Run run, Connection connection) throws RdvException {
    boolean exceptionOccurred = true;

    try {
      PreparedStatement updateRun = connection.prepareStatement(UPDATE_RUN_SQL);

      updateRun.setTimestamp(1, getTimestamp(run.getStartTime()), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      updateRun.setTimestamp(2, getTimestamp(run.getEndTime()), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      updateRun.setString(3, run.getState());
      updateRun.setString(4, run.getHostname());
      File outputPath = run.getRemoteOutputPath();
      updateRun.setString(5, outputPath == null ? null : outputPath.getPath());
      updateRun.setString(6, run.getOutputHost());
      updateRun.setString(7, run.getRunnerClass());
      updateRun.setInt(8, run.getRunId()); // this is for the where clause, we don't update this field.
      updateRun.executeUpdate();
      connection.commit();
      exceptionOccurred = false;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException thrown. Rolling back transaction.", e);
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      Utils.rollback(connection);
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  /**
   * Inserts the provided runs into the database, including their parameters.
   * Runs will be marked as 'scheduled'.
   *
   * @param runs runs to insert into the db
   * @throws RdvException if an error occurs inserting the runs
   */
  public synchronized void insertRuns(List<? extends Run> runs) throws RdvException {
    LOG.info("Saving new runs to database.");
    Connection connection = connectionFactory.createConnection();

    boolean exceptionOccurred = true;
    try {
      ResultSet rs = connection.prepareStatement("select nextval('runs_run_id_seq')").executeQuery();
      rs.next();
      int nextRunId = rs.getInt(1);
      connection.prepareStatement("select setval('runs_run_id_seq', " + (nextRunId + runs.size()) +
          ", false)").execute();
          // false here indicates that the next sequence number returned by 'select nextval' will be
          // nextRunId + runs.size(), as opposed to nextRunId + runs.size() + 1

      PreparedStatement insertRun = connection.prepareStatement(INSERT_RUN_SQL);
      PreparedStatement insertParams = connection.prepareStatement(ParametersDao.INSERT_PARAM_SQL);

      for (Run run : runs) {
        run.setRunId(nextRunId);
        insertRun.setInt(1, nextRunId);
        insertRun.setString(2, "scheduled");
        insertRun.setString(3, run.getRevision());
        insertRun.setString(4, run.getProjectName());
        insertRun.setString(5, run.getScenarioName());
        insertRun.setString(6, run.getFlags());
        insertRun.setString(7, run.getRunset());
        insertRun.setString(8, run.getClusterName());
        insertRun.setString(9, run.getRunnerClass());
        insertRun.addBatch();
        parametersDao.batchInsertParams(run.getRunId(), run.getParameters(), insertParams);
        nextRunId++;
      }

      insertRun.executeBatch();
      insertParams.executeBatch();

      connection.commit();
      exceptionOccurred = false;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException thrown. Rolling back transaction.", e);
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      Utils.rollback(connection);
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  /**
   * Prints the set of matching runs in the database to stdout.
   *
   * @param states         list of states to be matched (using boolean OR) (may be empty or null)
   * @param hostname       hostname to match, may be null.
   * @param runset         runset to match, may be null
   * @param runIds         list of run ids to match, may be null or empty
   * @param truncateOutput if the output should be truncated
   * @param outputType     output format
   * @throws RdvException if the runs cannot be loaded
   */
  public synchronized void printRuns(List<String> states, String hostname, String runset, List<Integer> runIds,
      boolean truncateOutput, Utils.OutputType outputType) throws RdvException {
    Utils.printResultSet(loadRuns(states, hostname, runset, runIds), truncateOutput, outputType);
  }

  public synchronized List<Run> getRuns(List<String> states, String hostname, String runset, List<Integer> runIds)
      throws RdvException {
    ResultSet resultSet = loadRuns(states, hostname, runset, runIds);
    List<Run> runs = Lists.newArrayList();
    try {
      while (resultSet.next()) {
        runs.add(runFromResultSet(resultSet, true));
      }
      return runs;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      throw new RdvException(e);
    }
  }

  private static Timestamp getTimestamp(Date time) {
    return time == null ? null : new Timestamp(time.getTime());
  }

  private ResultSet loadRuns(List<String> states, String hostname, String runset, List<Integer> runIds)
      throws RdvException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;

    try {
      StringBuilder sql = new StringBuilder("SELECT * FROM runs WHERE 1=1 ");
      if (states != null && !states.isEmpty()) {
        sql.append("AND state = ANY(?) ");
      }
      if (hostname != null) {
        sql.append("AND hostname = ? ");
      }
      if (runset != null) {
        sql.append("AND runset LIKE ? ");
      }
      if (runIds != null && !runIds.isEmpty()) {
        sql.append("AND run_id = ANY(?) ");
      }
      sql.append("ORDER BY run_id ASC");
      PreparedStatement statement = connection.prepareStatement(sql.toString());
      int fieldCounter = 1;
      if (states != null && !states.isEmpty()) {
        statement.setArray(fieldCounter++, connection.createArrayOf("text", states.toArray()));
      }
      if (hostname != null) {
        statement.setString(fieldCounter++, hostname);
      }
      if (runset != null) {
        statement.setString(fieldCounter++, runset);
      }
      if (runIds != null && !runIds.isEmpty()) {
        statement.setArray(fieldCounter, connection.createArrayOf("int", runIds.toArray()));
      }
      statement.execute();
      ResultSet resultSet = statement.getResultSet();
      exceptionOccurred = false;
      return resultSet;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  // TODO(michaell): This is inefficient for cases where we're loading multiple runs,
  // as it does a db call for each run.
  // If this turns out to be a problem, it may be worth switching to an ORM such as Hibernate.
  private Parameters loadParameters(int runId) throws RdvException {
    return parametersDao.loadFromDatabase(runId);
  }

  /**
   * Create a Run from a resultset row
   *
   * @param resultSet      resultSet must have active record (ie have called next() which returned true)
   * @param withParameters if true, also load the parameters for the run
   * @return newly created Run
   */
  private Run runFromResultSet(ResultSet resultSet, boolean withParameters) throws SQLException, RdvException {
    int runId = resultSet.getInt("run_id");
    Parameters parameters = withParameters ? loadParameters(runId) : Parameters.EMPTY_PARAMETERS;
    Run run = new Run(runId, resultSet.getString("project_name"), resultSet.getString("scenario_name"),
        resultSet.getString("code_version"), resultSet.getString("command_flags"), parameters,
        resultSet.getString("state"), resultSet.getString("runset"), resultSet.getString("cluster_name"),
        resultSet.getString("runner_class"));
    String outputPath = resultSet.getString("output_path");
    if (outputPath != null) {
      run.setRemoteOutputPath(new File(outputPath));
    }
    run.setOutputHost(resultSet.getString("output_host"));
    return run;
  }
}
