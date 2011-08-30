package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.RdvException;
import com.google.common.collect.Lists;

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
   * @param truncateOutput if the output columns should be truncated
   * @throws au.edu.rmit.tzar.api.RdvException
   *          if the result set can't be read
   */
  public static void printResultSet(ResultSet rs, boolean truncateOutput) throws RdvException {
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numColumns = rsmd.getColumnCount();

      // Get the column names; column indices start from 1
      List<String> columnNames = Lists.newArrayList();
      for (int i = 0; i < numColumns; i++) {
        columnNames.add(rsmd.getColumnName(i + 1));
      }

      TablePrinter printer = new TablePrinter(columnNames, truncateOutput);

      while (rs.next()) {
        String[] values = new String[numColumns];
        for (int i = 0; i < numColumns; i++) {
          String val = rs.getString(i + 1);
          values[i] = val == null ? "null" : val;
        }

        printer.addRow(values);
      }
      printer.print();
    } catch (SQLException e) {
      throw new RdvException(e);
    }
  }
}
