package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.CodeSource;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle Run management, including loading and persisting to the database.
 */
public class RunDao {
  private static final Logger LOG = Logger.getLogger(RunDao.class.getName());

  public static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  @VisibleForTesting
  static final String INSERT_RUN_SQL = "INSERT INTO runs (run_id, state, model_url, model_repo_type, model_revision, " +
      "project_name, scenario_name, runner_flags, runset, cluster_name, runner_class) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  @VisibleForTesting
  static final String NEXT_RUN_SQL = "SELECT run_id, state, model_url, model_repo_type, model_revision, project_name, " +
      "scenario_name, runner_flags, runset, cluster_name, output_path, output_host, runner_class FROM runs " +
      "WHERE state='scheduled' AND runset LIKE ? AND cluster_name = ? ORDER BY run_id ASC LIMIT 1";
  @VisibleForTesting
  static final String UPDATE_RUN_SQL = "UPDATE runs SET run_start_time = ?, run_end_time = ?, state = ?, " +
      "hostname = ?, host_ip = ?, output_path = ?, output_host = ? where run_id = ?";
  // select for update locks the row in question for modification, so that we can guarantee
  // than another node does not write to the row before we do
  @VisibleForTesting
  static final String SELECT_RUN_SQL = "SELECT state FROM runs where run_id = ? FOR UPDATE";
  private final ParametersDao parametersDao;
  private final ConnectionFactory connectionFactory;

  public RunDao(ConnectionFactory connectionFactory, ParametersDao parametersDao) throws TzarException {
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
   * @throws TzarException if something goes wrong executing the run
   */
  public synchronized Run getNextRun(String runset, String clusterName) throws TzarException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      connection.setAutoCommit(true);
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
      throw new TzarException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  public boolean markRunInProgress(Run run) throws TzarException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      connection.setAutoCommit(false);
      PreparedStatement selectRun = connection.prepareStatement(SELECT_RUN_SQL);
      selectRun.setInt(1, run.getRunId());
      ResultSet resultSet = selectRun.executeQuery();
      if (!resultSet.next()) {
        throw new TzarException("Expected to find record matching run_id: " + run.getRunId());
      }
      String status = resultSet.getString("state");
      if (!"scheduled".equals(status)) {
        LOG.fine("Expected run to have status 'scheduled', but status was: " + status + ". This probably indicates" +
            " that another node has grabbed the run.");
        return false;
      }
      persistRun(run, connection);
      connection.commit();
      exceptionOccurred = false;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException thrown. Rolling back transaction.", e);
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      Utils.rollback(connection);
      throw new TzarException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
    return true;
  }

  public synchronized void persistRun(Run run) throws TzarException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      connection.setAutoCommit(false);
      persistRun(run, connection);
      connection.commit();
      exceptionOccurred = false;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException thrown. Rolling back transaction.", e);
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      Utils.rollback(connection);
      throw new TzarException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  private void persistRun(Run run, Connection connection) throws SQLException {
    PreparedStatement updateRun = connection.prepareStatement(UPDATE_RUN_SQL);

    updateRun.setTimestamp(1, getTimestamp(run.getStartTime()), UTC);
    updateRun.setTimestamp(2, getTimestamp(run.getEndTime()), UTC);
    updateRun.setString(3, run.getState().name().toLowerCase());
    updateRun.setString(4, run.getHostname());
    updateRun.setString(5, run.getHostIp());
    File outputPath = run.getRemoteOutputPath();
    updateRun.setString(6, outputPath == null ? null : outputPath.getPath());
    updateRun.setString(7, run.getOutputHost());
    updateRun.setInt(8, run.getRunId()); // this is for the where clause, we don't update this field.
    updateRun.executeUpdate();
  }

  /**
   * Inserts the provided runs into the database, including their parameters.
   * Runs will be marked as 'scheduled'.
   *
   * @param runs runs to insert into the db
   * @throws TzarException if an error occurs inserting the runs
   */
  public synchronized void insertRuns(List<? extends Run> runs) throws TzarException {
    LOG.info("Saving new runs to database.");
    Connection connection = connectionFactory.createConnection();

    boolean exceptionOccurred = true;
    try {
      connection.setAutoCommit(false);

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
        CodeSourceImpl codeSource = run.getCodeSource();
        run.setRunId(nextRunId);
        insertRun.setInt(1, nextRunId);
        insertRun.setString(2, run.getState().name().toLowerCase());
        insertRun.setString(3, codeSource.getSourceUri().toString());
        insertRun.setString(4, codeSource.getRepositoryType().toString());
        insertRun.setString(5, codeSource.getRevision());
        insertRun.setString(6, run.getProjectName());
        insertRun.setString(7, run.getScenarioName());
        insertRun.setString(8, run.getRunnerFlags());
        insertRun.setString(9, run.getRunset());
        insertRun.setString(10, run.getClusterName());
        insertRun.setString(11, run.getRunnerClass());
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
      throw new TzarException(e);
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
   * @throws TzarException if the runs cannot be loaded
   */
  public synchronized void printRuns(List<String> states, String hostname, String runset, List<Integer> runIds,
      boolean truncateOutput, Utils.OutputType outputType) throws TzarException {
    Utils.printResultSet(loadRuns(states, hostname, runset, runIds), truncateOutput, outputType);
  }

  public synchronized List<Run> getRuns(List<String> states, String hostname, String runset, List<Integer> runIds)
      throws TzarException {
    ResultSet resultSet = loadRuns(states, hostname, runset, runIds);
    List<Run> runs = Lists.newArrayList();
    try {
      while (resultSet.next()) {
        runs.add(runFromResultSet(resultSet, true));
      }
      return runs;
    } catch (SQLException e) {
      LOG.log(Level.WARNING, "SQLException caused by:", e.getNextException());
      throw new TzarException(e);
    }
  }

  private static Timestamp getTimestamp(Date time) {
    return time == null ? null : new Timestamp(time.getTime());
  }

  private ResultSet loadRuns(List<String> states, String hostname, String runset, List<Integer> runIds)
      throws TzarException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;

    try {
      connection.setAutoCommit(true);
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
      throw new TzarException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  // TODO(michaell): This is inefficient for cases where we're loading multiple runs,
  // as it does a db call for each run.
  // If this turns out to be a problem, it may be worth switching to an ORM such as Hibernate.
  private Parameters loadParameters(int runId) throws TzarException {
    return parametersDao.loadFromDatabase(runId);
  }

  /**
   * Create a Run from a resultset row
   *
   * @param resultSet      resultSet must have active record (ie have called next() which returned true)
   * @param withParameters if true, also load the parameters for the run
   * @return newly created Run
   */
  private Run runFromResultSet(ResultSet resultSet, boolean withParameters) throws SQLException, TzarException {
    int runId = resultSet.getInt("run_id");
    Parameters parameters = withParameters ? loadParameters(runId) : Parameters.EMPTY_PARAMETERS;
    String modelUrlString = resultSet.getString("model_url");
    URI modelUri;
    try {
      modelUri = new URI(modelUrlString);
    } catch (URISyntaxException e) {
      throw new TzarException("model_url in database for run: " + runId + " was not a valid URI. Value was: " +
          modelUrlString + ". Error was: " + e.getMessage());
    }
    CodeSourceImpl codeSource = new CodeSourceImpl(modelUri,
        CodeSourceImpl.RepositoryType.valueOf(resultSet.getString("model_repo_type").toUpperCase()),
        resultSet.getString("model_revision"));

    // FIXME: load and save libraries to / from db.
    ImmutableMap<String, CodeSource> libraries = ImmutableMap.of();

    Run.ProjectInfo projectInfo = new Run.ProjectInfo(resultSet.getString("project_name"), codeSource,
        libraries, resultSet.getString("runner_class"), resultSet.getString("runner_flags"));
    Run run = new Run(projectInfo, resultSet.getString("scenario_name"))
        .setRunId(runId)
        .setParameters(parameters)
        .setState(Run.State.valueOf(resultSet.getString("state").toUpperCase()))
        .setRunset(resultSet.getString("runset"))
        .setClusterName(resultSet.getString("cluster_name"));
    String outputPath = resultSet.getString("output_path");
    if (outputPath != null) {
      run.setRemoteOutputPath(new File(outputPath));
    }
    run.setOutputHost(resultSet.getString("output_host"));
    return run;
  }
}
