package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.RdvException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Factory to create database access objects.
 */
public class DaoFactory {
  private final ConnectionFactory connectionFactory;

  public DaoFactory(String dbUrl) {
    this.connectionFactory = new ConnectionFactory(dbUrl);
  }

  public RunDao createRunDao() throws RdvException {
    try {
      Connection connection = connectionFactory.createConnection();
      return new RunDao(connection, new ParametersDao(connection));
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  /**
   * Factory to create database connections.
   */
  private static class ConnectionFactory {
    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());

    private final String dbString;

    public ConnectionFactory(String dbString) {
      this.dbString = dbString;
    }

    public Connection createConnection() throws SQLException {
      LOG.info("Creating connection to DB: " + dbString);
      return DriverManager.getConnection(dbString);
    }
  }
}
