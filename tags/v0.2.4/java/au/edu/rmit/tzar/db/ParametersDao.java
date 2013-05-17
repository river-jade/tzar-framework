package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

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
   * Insert the provided parameters into the database, to be associated with the
   * provided runId.
   *
   * @param runId
   * @param parameters
   * @throws RdvException
   */
  public void insertParams(int runId, Parameters parameters) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    try {
      PreparedStatement insertParam = connection.prepareStatement(INSERT_PARAM_SQL);
      batchInsertParams(runId, parameters, insertParam);
      insertParam.executeBatch();
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  /**
   * Loads the parameters for the run with runId from the database.
   *
   * @param runId
   * @return
   * @throws SQLException
   */
  public Parameters loadFromDatabase(int runId) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      PreparedStatement loadParams = connection.prepareStatement(LOAD_PARAMS_SQL);
      loadParams.setInt(1, runId);
      ResultSet resultSet = loadParams.executeQuery();
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
      Parameters parameters = Parameters.createParameters(variables, inputFiles, outputFiles);
      exceptionOccurred = false;
      return parameters;
    } catch (SQLException e) {
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  /**
   * Prints the parameters corresponding to the provided run id to stdout.
   *
   * @param runId          the run whose parameters are to be printed
   * @param truncateOutput if the output fields should be truncated for formatting
   * @param outputType     output format
   * @throws RdvException if the parameters can't be loaded
   */
  public void printParameters(int runId, boolean truncateOutput, Utils.OutputType outputType) throws RdvException {
    Connection connection = connectionFactory.createConnection();
    boolean exceptionOccurred = true;
    try {
      PreparedStatement loadParams = connection.prepareStatement(LOAD_PARAMS_SQL);
      loadParams.setInt(1, runId);
      Utils.printResultSet(loadParams.executeQuery(), truncateOutput, outputType);
      exceptionOccurred = false;
    } catch (SQLException e) {
      throw new RdvException(e);
    } finally {
      Utils.close(connection, exceptionOccurred);
    }
  }

  void batchInsertParams(int runId, Parameters parameters, PreparedStatement insertParam)
      throws SQLException {
    insertParams(runId, parameters.getOutputFiles(), "output_file", insertParam);
    insertParams(runId, parameters.getInputFiles(), "input_file", insertParam);
    insertParams(runId, parameters.getVariables(), "variable", insertParam);
  }

  private void insertParams(int runId, Map<String, ?> inputFiles, String paramType, PreparedStatement insertParam)
      throws SQLException {
    for (Map.Entry<String, ?> entry : inputFiles.entrySet()) {
      insertParam.setInt(1, runId);
      insertParam.setString(2, entry.getKey());
      Object value = entry.getValue();
      insertParam.setString(3, value.toString());
      insertParam.setString(4, paramType);
      insertParam.setString(5, DataType.getType(value).name);
      insertParam.addBatch();
    }
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
    };

    private final String name;
    private static final Map<String, DataType> map = Maps.newHashMap();

    static {
      for (DataType dataType : DataType.values()) {
        map.put(dataType.name, dataType);
      }
    }

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
      } else {
        return STRING;
      }
    }

    public abstract Object newInstance(String value);

    public static DataType fromName(String name) {
      if (!map.containsKey(name)) {
        throw new IllegalArgumentException("Parameter type: " + name + " not recognised.");
      }
      return map.get(name);
    }
  }
}
