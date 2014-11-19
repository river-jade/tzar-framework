package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.CodeSource;
import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
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
import java.util.concurrent.Callable;
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
  static final String NEXT_RUN_SQL = "SELECT run_id, state, model_url, model_repo_type, model_revision, " +
      "project_name, scenario_name, runner_flags, runset, cluster_name, output_path, output_host, runner_class " +
      "FROM runs WHERE state='scheduled' AND runset LIKE ? AND cluster_name = ? ORDER BY run_id ASC LIMIT 1";
  @VisibleForTesting
  static final String UPDATE_RUN_SQL = "UPDATE runs SET run_start_time = ?, run_end_time = ?, state = ?, " +
      "hostname = ?, host_ip = ?, output_path = ?, output_host = ? where run_id = ?";
  // select for update locks the row in question for modification, so that we can guarantee
  // than another node does not write to the row before we do
  @VisibleForTesting
  static final String SELECT_RUN_SQL = "SELECT state FROM runs where run_id = ? FOR UPDATE";

  static final String RUNSET_EXISTS = "SELECT count(*) from runs where runset = ?";

  private final ParametersDao parametersDao;
  private final LibraryDao libraryDao;
  private final ConnectionFactory connectionFactory;

  public RunDao(ConnectionFactory connectionFactory, ParametersDao parametersDao, LibraryDao libraryDao)
      throws TzarException {
    this.connectionFactory = connectionFactory;
    this.parametersDao = parametersDao;
    this.libraryDao = libraryDao;
  }

  /**
   * Polls the database for the next scheduled run. If a scheduled run is found, the run is marked as
   * 'in_progress' and persisted to the database. This is done in a single transaction, to avoid two nodes
   * from executing the same run.
   *
   * @param runset      runset to filter by or null to poll for any runset
   * @param clusterName we only poll for runs scheduled for the current cluster. Not null, but may be empty.
   * @return true if a run was found, false otherwise
   * @throws TzarException if something goes wrong executing the run
   */
  public synchronized Optional<Run> getNextRun(final Optional<String> runset, final String clusterName)
      throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    return Utils.executeInTransaction(new Callable<Optional<Run>>() {
      @Override
      public Optional<Run> call() throws Exception {
        PreparedStatement selectNextRun = connection.prepareStatement(NEXT_RUN_SQL);
        selectNextRun.setString(1, runset.or("%"));
        selectNextRun.setString(2, clusterName);
        ResultSet resultSet = selectNextRun.executeQuery();

        if (resultSet.next()) {
          return Optional.of(runFromResultSet(resultSet, true, connection));
        } else {
          return Optional.absent();
        }
      }
    }, connection);
  }

  public boolean markRunInProgress(final Run run) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    return Utils.executeInTransaction(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        PreparedStatement selectRun = connection.prepareStatement(SELECT_RUN_SQL);
        selectRun.setInt(1, run.getRunId());
        ResultSet resultSet = selectRun.executeQuery();
        if (!resultSet.next()) {
          throw new TzarException("Expected to find record matching run_id: " + run.getRunId());
        }
        Run.State status = Run.State.valueOf(resultSet.getString("state").toUpperCase());
        if (status != Run.State.SCHEDULED) {
          LOG.fine("Expected run to have status 'scheduled', but status was: " + status + ". This probably indicates" +
              " that another node has grabbed the run.");
          return false;
        }
        persistRun(run, connection);
        return true;
      }
    }, connection);
  }

  public synchronized void persistRun(final Run run) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    Utils.executeInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        persistRun(run, connection);
        return null;
      }
    }, connection);
  }

  private void persistRun(Run run, Connection connection) throws SQLException {
    PreparedStatement updateRun = connection.prepareStatement(UPDATE_RUN_SQL);

    updateRun.setTimestamp(1, getTimestamp(run.getStartTime()), UTC);
    updateRun.setTimestamp(2, getTimestamp(run.getEndTime()), UTC);
    updateRun.setString(3, run.getState().name().toLowerCase());
    updateRun.setString(4, run.getHostname());
    updateRun.setString(5, run.getHostIp());
    File outputPath = run.getRemoteOutputPath();
    updateRun.setString(6, outputPath == null ? null : outputPath.getAbsolutePath());
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
  public synchronized void insertRuns(final List<? extends Run> runs) throws TzarException {
    LOG.info("Saving new runs to database.");
    final Connection connection = connectionFactory.createConnection();

    Utils.executeInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ResultSet rs = connection.prepareStatement("select nextval('runs_run_id_seq')").executeQuery();
        rs.next();
        int nextRunId = rs.getInt(1);
        connection.prepareStatement("select setval('runs_run_id_seq', " + (nextRunId + runs.size()) +
            ", false)").execute();
        // false here indicates that the next sequence number returned by 'select nextval' will be
        // nextRunId + runs.size(), as opposed to nextRunId + runs.size() + 1

        PreparedStatement insertRun = connection.prepareStatement(INSERT_RUN_SQL);

        // defer constraint checking because the run won't exist when we add the library. this is safe
        // because it's in a transaction. the constraint will be checked once the transaction is committed.
        connection.prepareStatement("SET CONSTRAINTS run_libraries_run_id_fkey DEFERRED").execute();
        ParametersDao.BatchInserter batchInserter = parametersDao.createBatchInserter(connection);
        for (Run run : runs) {
          CodeSource codeSource = run.getCodeSource();
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
          batchInserter.insertParams(run.getRunId(), run.getParameters());
          libraryDao.associateLibraries(run.getLibraries(), nextRunId, connection);
          nextRunId++;
        }

        insertRun.executeBatch();
        batchInserter.executeBatch();
        return null;
      }
    }, connection);
  }

  /**
   * Prints the set of matching runs in the database to stdout.
   *
   * @param states         list of states to be matched (using boolean OR) (may be empty)
   * @param hostname       hostname to match
   * @param runset         runset to match
   * @param runIds         list of run ids to match, may be empty
   * @param truncateOutput if the output should be truncated
   * @param outputType     output format
   * @throws TzarException if the runs cannot be loaded
   */
  public synchronized void printRuns(final List<String> states, final Optional<String> hostname,
      final Optional<String> runset, final List<Integer> runIds, final boolean truncateOutput,
      final Utils.OutputType outputType) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    Utils.executeInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ResultSet rs = findRuns(states, hostname, runset, runIds, connection);
        Utils.printResultSet(rs, truncateOutput, outputType);
        return null;
      }
    }, connection);
  }

  public synchronized List<Run> getRuns(final List<String> states, final Optional<String> hostname,
      final Optional<String> runset, final List<Integer> runIds) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    return Utils.executeInTransaction(new Callable<List<Run>>() {
      @Override
      public List<Run> call() throws Exception {
        ResultSet resultSet = findRuns(states, hostname, runset, runIds, connection);
        List<Run> runs = Lists.newArrayList();
        while (resultSet.next()) {
          runs.add(runFromResultSet(resultSet, true, connection));
        }
        return runs;
      }
    }, connection);
  }

  private static Timestamp getTimestamp(Date time) {
    return time == null ? null : new Timestamp(time.getTime());
  }

  private ResultSet findRuns(List<String> states, Optional<String> hostname, Optional<String> runset,
      List<Integer> runIds, Connection connection) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT * FROM runs WHERE 1=1 ");
    if (!states.isEmpty()) {
      sql.append("AND state = ANY(?) ");
    }
    if (hostname.isPresent()) {
      sql.append("AND hostname = ? ");
    }
    if (runset.isPresent()) {
      sql.append("AND runset LIKE ? ");
    }
    if (!runIds.isEmpty()) {
      sql.append("AND run_id = ANY(?) ");
    }
    sql.append("ORDER BY run_id ASC");
    PreparedStatement statement = connection.prepareStatement(sql.toString());
    int fieldCounter = 1;
    if (!states.isEmpty()) {
      statement.setArray(fieldCounter++, connection.createArrayOf("text", states.toArray()));
    }
    if (hostname.isPresent()) {
      statement.setString(fieldCounter++, hostname.get());
    }
    if (runset.isPresent()) {
      statement.setString(fieldCounter++, runset.get());
    }
    if (!runIds.isEmpty()) {
      statement.setArray(fieldCounter, connection.createArrayOf("int", runIds.toArray()));
    }
    statement.execute();
    return statement.getResultSet();
  }

  /**
   * Create a Run from a resultset row. This looks up the libraries for the run, and (optionally)
   * the parameters.
   *
   * @param resultSet      resultSet must have active record (ie have called next() which returned true)
   * @param withParameters if true, also load the parameters for the run
   * @param connection     connection object, to be used for looking up params and libs
   * @return newly created Run
   */
  private Run runFromResultSet(ResultSet resultSet, boolean withParameters, Connection connection)
      throws SQLException, TzarException {
    int runId = resultSet.getInt("run_id");

    // TODO(river): This is inefficient for cases where we're loading multiple runs,
    // as it does a db call for each run.
    // If this turns out to be a problem, it may be worth switching to an ORM such as Hibernate.
    Parameters parameters = withParameters ? parametersDao.loadFromDatabase(runId, connection)
        : Parameters.EMPTY_PARAMETERS;
    ImmutableMap<String, CodeSource> libraries = libraryDao.getLibraries(runId, connection);
    String modelUrlString = resultSet.getString("model_url");
    URI modelUri;
    try {
      modelUri = new URI(modelUrlString);
    } catch (URISyntaxException e) {
      throw new TzarException("model_url in database for run: " + runId + " was not a valid URI. Value was: " +
          modelUrlString + ". Error was: " + e.getMessage());
    }
    CodeSourceImpl.RepositoryTypeImpl repositoryType = CodeSourceImpl.RepositoryTypeImpl.valueOf(resultSet.getString
        ("model_repo_type").toUpperCase());
    CodeSourceImpl codeSource = new CodeSourceImpl(modelUri, repositoryType, resultSet.getString("model_revision"),
        true /* by default we force download of model code */);

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

  /**
   * Check if a runset already exists in the database.
   *
   * @param runset the name of the runset to look for
   * @return true if the runset exists
   * @throws TzarException
   */
  public boolean runsetExists(final String runset) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    return Utils.executeSqlStatement(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        PreparedStatement statement = connection.prepareStatement(RUNSET_EXISTS);
        statement.setString(1, runset);
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        resultSet.next();
        return resultSet.getInt(1) > 0;
      }
    }, connection);
  }
}
