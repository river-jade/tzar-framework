package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data access object for Parameters. Provides functionality for parameters to be loaded from
 * and persisted to the database.
 */
public class ParametersDao {
  private static final Logger LOG = Logger.getLogger(ParametersDao.class.getName());

  @VisibleForTesting
  static final String LOAD_PARAMS_SQL = "SELECT p.param_name, p.param_value, p.param_type, p.data_type " +
      "FROM runs as r INNER JOIN run_params as p ON r.run_id = p.run_id WHERE r.run_id = ?";

  @VisibleForTesting
  static final String INSERT_PARAM_SQL = "INSERT INTO run_params (run_id, param_name, param_value, param_type, " +
      "data_type) VALUES (?, ?, ?, ?, ?)";

  private final ConnectionFactory connectionFactory;

  public ParametersDao(ConnectionFactory connectionFactory) throws SQLException {
    this.connectionFactory = connectionFactory;
  }

  /**
   * Factory method for BatchInserter. Facilitates inserting or updating records in batches.
   * @param connection db connection
   * @return a new BatchInserter
   * @throws SQLException
   */
  BatchInserter createBatchInserter(Connection connection) throws SQLException {
    return new BatchInserter(connection.prepareStatement(INSERT_PARAM_SQL));
  }

  /**
   * Insert the provided parameters into the database, to be associated with the
   * provided runId.
   *
   * @param runId
   * @param parameters
   * @throws TzarException
   */
  public void insertParams(final int runId, final Parameters parameters) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    Utils.executeInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        BatchInserter batchInserter = createBatchInserter(connection);
        batchInserter.insertParams(runId, parameters);
        batchInserter.executeBatch();
        return null;
      }
    }, connection);
  }

  /**
   * Loads the parameters for the run with runId from the database.
   *
   * @param runId
   * @return
   * @throws SQLException
   */
  public Parameters loadFromDatabase(final int runId) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    return Utils.executeInTransaction(new Callable<Parameters>() {
      @Override
      public Parameters call() throws Exception {
        return loadFromDatabase(runId, connection);
      }
    }, connection);
  }

  Parameters loadFromDatabase(int runId, Connection connection) throws SQLException, TzarException {
    PreparedStatement loadParams = connection.prepareStatement(LOAD_PARAMS_SQL);
    loadParams.setInt(1, runId);
    ResultSet resultSet = loadParams.executeQuery();
    // TODO(river): remove the redundant references to inputFiles and outputFiles, now that we don't
    // use these.
    Map<String, Object> variables = Maps.newLinkedHashMap();
    Map<String, String> inputFiles = Maps.newLinkedHashMap();
    Map<String, String> outputFiles = Maps.newLinkedHashMap();
    while (resultSet.next()) {
      DataType type = DataType.fromName(resultSet.getString("data_type"));
      Object param = type.newInstance(resultSet.getString("param_value"));
      String paramType = resultSet.getString("param_type");
      if ("variable".equals(paramType)) {
        addParam(resultSet, variables, param);
      } else if ("input_file".equals(paramType)) {
        addParam(resultSet, inputFiles, (String) param);
      } else if ("output_file".equals(paramType)) {
        addParam(resultSet, outputFiles, (String) param);
      }
    }
    return Parameters.createParameters(variables);
  }

  /**
   * Prints the parameters corresponding to the provided run id to stdout.
   *
   * @param runId          the run whose parameters are to be printed
   * @param truncateOutput if the output fields should be truncated for formatting
   * @param outputType     output format
   * @throws TzarException if the parameters can't be loaded
   */
  public void printParameters(final int runId, final boolean truncateOutput, final Utils.OutputType outputType) throws TzarException {
    final Connection connection = connectionFactory.createConnection();
    Utils.executeInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        PreparedStatement loadParams = connection.prepareStatement(LOAD_PARAMS_SQL);
        loadParams.setInt(1, runId);
        Utils.printResultSet(loadParams.executeQuery(), truncateOutput, outputType);
        return null;
      }
    }, connection);
  }

  private static <T> void addParam(ResultSet resultSet, Map<String, T> paramMap, T param) throws SQLException {
    paramMap.put(resultSet.getString("param_name"), param);
  }

  private enum DataType {
    FLOAT("float") {
      @Override
      public Object newInstance(String value) {
        return new BigDecimal(value);
      }
    },
    STRING("str") {
      @Override
      public Object newInstance(String value) {
        return value;
      }
    },
    INT("int") {
      @Override
      public Object newInstance(String value) {
        return Integer.valueOf(value);
      }
    },
    BOOL("bool") {
      @Override
      public Object newInstance(String value) {
        return Boolean.valueOf(value);
      }
    },
    LIST("list") {
      @Override
      public List<String> newInstance(String value) throws TzarException {
        Matcher matcher = LIST_PATTERN.matcher(value);
        if (!matcher.matches()) {
          throw new TzarException("List data was not in expected format.");
        }
        String commaDelimited = matcher.group(1);
        return Lists.newArrayList(SPLITTER.split(commaDelimited));
      }

      @Override
      public String toString(Object value) {
        return "[" + JOINER.join((Iterable) value) + "]";
      }
    };

    private final String name;
    private static final Map<String, DataType> map = Maps.newHashMap();

    static {
      for (DataType dataType : DataType.values()) {
        map.put(dataType.name, dataType);
      }
    }

    private static final Pattern LIST_PATTERN = Pattern.compile("^\\[(.*)\\]$");
    private static final Joiner JOINER = Joiner.on(',');
    private static final Splitter SPLITTER = Splitter.on(',');

    private DataType(String name) {
      this.name = name;
    }

    public static DataType getType(Object value) {
      if (value instanceof Integer || value instanceof Long) {
        return INT;
      } else if (value instanceof Boolean) {
        return BOOL;
      } else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
        return FLOAT;
      } else if (value instanceof Iterable) {
        return LIST;
      } else {
        return STRING;
      }
    }

    public abstract Object newInstance(String value) throws TzarException;

    public String toString(Object value) {
      return value.toString();
    }

    public static DataType fromName(String name) {
      if (!map.containsKey(name)) {
        throw new IllegalArgumentException("Parameter type: " + name + " not recognised.");
      }
      return map.get(name);
    }
  }

  /**
   * Helper class for inserting parameters in a batch, which drastically reduces
   * database round trips.
   *
   * {@link #insertParams} may be called multiple times, but the database won't be updated until
   * {@link #executeBatch} is called.
   */
  class BatchInserter {
    private final PreparedStatement insertParam;

    public BatchInserter(PreparedStatement insertParam) {
      this.insertParam = insertParam;
    }

    public void insertParams(int runId, Parameters parameters) throws SQLException {
      for (Map.Entry<String, ?> entry : parameters.asMap().entrySet()) {
        Object value = entry.getValue();
        DataType type = DataType.getType(value);
        insertParam.setInt(1, runId);
        insertParam.setString(2, entry.getKey());
        insertParam.setString(3, type.toString(value));
        insertParam.setString(4, "variable");
        insertParam.setString(5, type.name);
        insertParam.addBatch();
      }
    }

    public void executeBatch() throws SQLException {
      insertParam.executeBatch();
    }
  }
}
