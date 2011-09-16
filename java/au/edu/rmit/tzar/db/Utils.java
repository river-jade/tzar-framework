package au.edu.rmit.tzar.db;

import au.com.bytecode.opencsv.CSVWriter;
import au.edu.rmit.tzar.api.RdvException;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Static database utility functions
 */
public class Utils {
  /**
   * Prints the provided result set to stdout in tabular form.
   *
   * @param rs             the resultset to print
   * @param truncateOutput if the output columns should be truncated. ignore unless outputType is PRETTY.
   * @param outputType     output format to print
   * @throws RdvException if the result set can't be read
   */
  public static void printResultSet(ResultSet rs, boolean truncateOutput, OutputType outputType) throws RdvException {
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
      throw new RdvException(e);
    } catch (IOException e) {
      throw new RdvException(e);
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
        values[i] = val == null ? "null" : val;
      }

      printer.addRow(values);
    }
    printer.print();
  }

  public enum OutputType {
    PRETTY,
    CSV
  }
}
