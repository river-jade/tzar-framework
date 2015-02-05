package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.repository.CodeSourceFactory;

import java.sql.SQLException;

/**
 * Factory to create database access objects.
 */
public class DaoFactory {
  private final ConnectionFactory connectionFactory;
  private final CodeSourceFactory codeSourceFactory;

  public DaoFactory(String dbUrl, CodeSourceFactory codeSourceFactory) throws TzarException {
    this.codeSourceFactory = codeSourceFactory;
    this.connectionFactory = new ConnectionFactory(dbUrl);
  }

  public RunDao createRunDao() throws TzarException {
    try {
      return new RunDao(connectionFactory, new ParametersDao(connectionFactory),
          new LibraryDao(codeSourceFactory), codeSourceFactory);
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
