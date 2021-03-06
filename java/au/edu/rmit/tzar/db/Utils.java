package au.edu.rmit.tzar.db;

import au.com.bytecode.opencsv.CSVWriter;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static database utility functions
 */
public class Utils {
  private static final Logger LOG = Logger.getLogger(Utils.class.getName());

  public static void close(Connection connection, boolean exceptionOccurred) throws TzarException {
    try {
      connection.close();
    } catch (SQLException e) {
      if (exceptionOccurred) {
        // we just log this new exception rather than rethrowing it,
        // so that we don't lose the original one...
        LOG.log(Level.SEVERE, "Unable to close JDBC Connection", e);
      } else {
        throw new TzarException("Unable to close JDBC Connection");
      }
    }
  }

  /**
   * Prints the provided result set to stdout in tabular form.
   *
   * @param rs             the resultset to print
   * @param truncateOutput if the output columns should be truncated. ignore unless outputType is PRETTY.
   * @param outputType     output format to print
   * @throws TzarException if the result set can't be read
   */
  public static void printResultSet(ResultSet rs, boolean truncateOutput, OutputType outputType) throws TzarException {
    try {
      switch (outputType) {
        case PRETTY:
          prettyPrintOutput(rs, truncateOutput);
          break;
        case CSV:
          printCsv(rs);
          break;
      }
    } catch (SQLException e) {
      throw new TzarException(e);
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  public static void rollback(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException e) {
      LOG.log(Level.SEVERE, "Exception occurred while rolling back transaction.", e);
    }
  }

  private static void printCsv(ResultSet rs) throws IOException, SQLException {
    OutputStreamWriter osw = new OutputStreamWriter(System.out);
    CSVWriter writer = new CSVWriter(osw, ',', '"');
    writer.writeAll(rs, true);
    writer.flush();
  }

  private static void prettyPrintOutput(ResultSet rs, boolean truncateOutput)
      throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();

    List<String> columnNames = Lists.newArrayList();
    // Get the column names; column indices start from 1
    for (int i = 0; i < numColumns; i++) {
      columnNames.add(rsmd.getColumnName(i + 1));
    }

    TablePrinter printer = new TablePrinter(columnNames, truncateOutput, System.out);
    int numCols = columnNames.size();

    while (rs.next()) {
      String[] values = new String[numCols];
      for (int i = 0; i < numCols; i++) {
        String val = rs.getString(i + 1);
        values[i] = (val == null ? "null" : val);
      }

      printer.addRow(values);
    }
    printer.print();
  }


  /**
   * Executes a method within a SQL transaction, correctly handling any thrown SQLExceptions and closing the connection
   * after the method completes.
   * @param callable the method to call
   * @param connection the database connection
   * @param <V> the return value
   * @return the value returned by the callable
   * @throws TzarException
   */
  public static <V> V executeInTransaction(final Callable<V> callable, final Connection connection) throws TzarException {
    return executeSqlStatement(new Callable<V>() {
      @Override
      public V call() throws Exception {
        V retVal = callable.call();
        connection.commit();
        return retVal;
      }
    }, connection);
  }

  /**
   * Execute a method that might throw a SQLException. For some reason, these exceptions are weird
   * in that the actual cause of the exception has to be explicitly unpacked. This method does that for callers.
   * @param callable the actual function to execute
   * @param connection the database connection
   * @param <V> the return type
   * @return whatever is returned by the callable
   * @throws TzarException if a sql exception is thrown
   */
  public static <V> V executeSqlStatement(Callable<V> callable, Connection connection) throws TzarException {
    boolean exceptionOccurred = true;
    V retVal;
    try {
      retVal = callable.call();
      exceptionOccurred = false;
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Exception thrown. Rolling back transaction.", e);
      if (e instanceof SQLException) {
        LOG.log(Level.WARNING, "SQLException caused by:", ((SQLException) e).getNextException());
      }
      rollback(connection);
      throw new TzarException(e);
    } finally {
      close(connection, exceptionOccurred);
    }
    return retVal;
  }

  public enum OutputType {
    PRETTY,
    CSV
  }
}
