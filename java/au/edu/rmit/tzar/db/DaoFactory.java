package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.RdvException;

import java.sql.SQLException;

/**
 * Factory to create database access objects.
 */
public class DaoFactory {
  private final ConnectionFactory connectionFactory;

  public DaoFactory(String dbUrl) throws RdvException {
    this.connectionFactory = new ConnectionFactory(dbUrl);
  }

  public RunDao createRunDao() throws RdvException {
    try {
      return new RunDao(connectionFactory, new ParametersDao(connectionFactory));
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }

  public ParametersDao createParametersDao() throws RdvException {
    try {
      return new ParametersDao(connectionFactory);
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }
}
