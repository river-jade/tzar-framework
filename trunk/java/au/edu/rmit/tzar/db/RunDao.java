package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.io.File;
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

  @VisibleForTesting
  static final String INSERT_RUN_SQL = "INSERT INTO runs (state, code_version, run_name, command_flags, runset) " +
      "VALUES (?, ?, ?, ?, ?)";

  @VisibleForTesting
  static final String NEXT_RUN_SQL = "SELECT run_id, state, code_version, run_name, command_flags, runset, " +
      "output_path, output_host FROM runs WHERE state='scheduled' AND runset LIKE ? ORDER BY run_id ASC LIMIT 1";

  @VisibleForTesting
  static final String UPDATE_RUN_SQL = "UPDATE runs SET run_start_time = ?, run_end_time = ?, state = ?, " +
      "hostname = ?, output_path = ?, output_host = ? where run_id = ?";

  @VisibleForTesting
  static final String SELECT_RUN_SQL = "SELECT run_id, state FROM runs WHERE run_id = ?";

  private final Connection connection;
  private final PreparedStatement selectNextRun;
  private final PreparedStatement updateRun;
  private final PreparedStatement insertRun;
  private final ParametersDao parametersDao;

  public RunDao(Connection connection, ParametersDao parametersDao) throws RdvException {
    try {
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      connection.setAutoCommit(false); // to enable transactions

      selectNextRun = connection.prepareStatement(NEXT_RUN_SQL, ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      updateRun = connection.prepareStatement(UPDATE_RUN_SQL);
      insertRun = connection.prepareStatement(INSERT_RUN_SQL, Statement.RETURN_GENERATED_KEYS);

      this.connection = connection;
      this.parametersDao = parametersDao;
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  /**
   * Polls the database for the next scheduled run.
   *
   * @param runset runset to filter by or null to poll for any runset
   * @return true if a run was found and executed, false otherwise
   * @throws RdvException if something goes wrong executing the run
   */
  public synchronized Run getNextRun(String runset) throws RdvException {
    try {
      connection.setAutoCommit(true);
      selectNextRun.setString(1, runset == null ? "%" : runset);
      ResultSet resultSet = selectNextRun.executeQuery();
      connection.setAutoCommit(false);

      if (resultSet.next()) {
        return runFromResultSet(resultSet, true);
      } else {
        return null;
      }
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  public synchronized void persistRun(Run run) throws RdvException {
    try {
      updateRun.setTimestamp(1, getTimestamp(run.getStartTime()), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      updateRun.setTimestamp(2, getTimestamp(run.getEndTime()), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      updateRun.setString(3, run.getState());
      updateRun.setString(4, run.getHostname());
      File outputPath = run.getOutputPath();
      updateRun.setString(5, outputPath == null ? null : outputPath.getAbsolutePath());
      updateRun.setString(6, run.getOutputHost());
      updateRun.setInt(7, run.getRunId());
      try {
        updateRun.executeUpdate();
        connection.commit();
      } catch (SQLException e) {
        LOG.log(Level.WARNING, "SQLException thrown. Rolling back transaction.", e);
        connection.rollback();
        throw new RdvException(e);
      }
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  /**
   * Inserts the provided runs into the database, including their parameters.
   * Runs will be marked as 'scheduled'.
   *
   * @param runs runs to insert into the db
   * @throws RdvException if an error occurs inserting the runs
   */
  public synchronized void insertRuns(Iterable<? extends Run> runs) throws RdvException {
    try {
      connection.setAutoCommit(false);
      for (Run run : runs) {
        insertRun.setString(1, "scheduled");
        insertRun.setString(2, run.getRevision());
        insertRun.setString(3, run.getName());
        insertRun.setString(4, run.getFlags());
        insertRun.setString(5, run.getRunset());
        insertRun.executeUpdate();
        ResultSet keys = insertRun.getGeneratedKeys();
        keys.next();
        int runId = keys.getInt("run_id");
        parametersDao.insertParams(runId, run.getParameters());
        run.setRunId(runId);
      }
      connection.commit();
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new RdvException(e);
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
   * @throws RdvException if the runs cannot be loaded
   */
  public synchronized void printRuns(List<String> states, String hostname, String runset, List<Integer> runIds,
      boolean truncateOutput) throws RdvException {
    Utils.printResultSet(loadRuns(states, hostname, runset, runIds), truncateOutput);
  }

  /**
   * Prints the run matching the provided run id and its run parameters to stdout.
   *
   * @param runId          the id of the run to print
   * @param truncateOutput if the output should be truncated to fit on screen
   * @throws RdvException if the run cannot be loaded
   */
  public synchronized void printRun(int runId, boolean truncateOutput) throws RdvException {
    Utils.printResultSet(loadRuns(null, null, null, Lists.newArrayList(runId)), truncateOutput);
    parametersDao.printParameters(runId, truncateOutput);
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
      throw new RdvException(e);
    }
  }

  private static Timestamp getTimestamp(Date time) {
    return time == null ? null : new Timestamp(time.getTime());
  }

  private ResultSet loadRuns(List<String> states, String hostname, String runset, List<Integer> runIds)
      throws RdvException {
    try {
      connection.setAutoCommit(false);
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
      return statement.getResultSet();
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  // TODO(michaell): This is inefficient for cases where we're loading multiple runs,
  // as it does a db call for each run.
  // If this turns out to be a problem, it may be worth switching to an ORM such as Hibernate.
  private Parameters loadParameters(int runId) throws SQLException {
    return parametersDao.loadFromDatabase(runId);
  }

  /**
   * Create a Run from a resultset row
   *
   * @param resultSet      resultSet must have active record (ie have called next() which returned true)
   * @param withParameters if true, also load the parameters for the run
   * @return newly created Run
   */
  private Run runFromResultSet(ResultSet resultSet, boolean withParameters) throws SQLException {
    int runId = resultSet.getInt("run_id");
    Parameters parameters = withParameters ? loadParameters(runId) : Parameters.EMPTY_PARAMETERS;
    Run run = new Run(runId, resultSet.getString("run_name"), resultSet.getString("code_version"),
        resultSet.getString("command_flags"), parameters, resultSet.getString("state"),
        resultSet.getString("runset"));
    String outputPath = resultSet.getString("output_path");
    if (outputPath != null) {
      run.setOutputPath(new File(outputPath));
    }
    run.setOutputHost(resultSet.getString("output_host"));
    return run;
  }


}
