package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.TzarException;

import java.sql.SQLException;

/**
 * Factory to create database access objects.
 */
public class DaoFactory {
  private final ConnectionFactory connectionFactory;

  public DaoFactory(String dbUrl) throws TzarException {
    this.connectionFactory = new ConnectionFactory(dbUrl);
  }

  public RunDao createRunDao() throws TzarException {
    try {
      return new RunDao(connectionFactory, new ParametersDao(connectionFactory));
    } catch (SQLException e) {
      throw new TzarException(e);
    }
  }

  public ParametersDao createParametersDao() throws TzarException {
    try {
      return new ParametersDao(connectionFactory);
    } catch (SQLException e) {
      throw new TzarException(e);
    }
  }
}
