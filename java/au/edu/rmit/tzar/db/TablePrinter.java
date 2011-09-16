package au.edu.rmit.tzar.db;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The table printer classes takes a matrix of data and prints it.
 * Original code taken from this thread:
 * http://stackoverflow.com/questions/5475896/system-out-println-from-database-into-a-table
 * Author: http://stackoverflow.com/users/330057/totalfrickinrockstarfrommars
 */
public class TablePrinter {
  private static final int MAX_WIDTH = 25;
  private final boolean truncateOutput;
  private final PrintStream out;

  /**
   * The row class represents one row of data.
   * Yes, it's just a wrapper for String[], but it helps
   * keep it simple.
   */
  private static class Row {
    String[] data;

    Row(String[] v) {
      data = v;
    }
  }

  /**
   * Contains column header and max width information
   */
  private static class Col {
    String name;
    int maxWidth;
  }

  // matrix information
  Col[] cols;
  ArrayList<Row> rows;

  /**
   * Constructor - pass in columns as an array, or hard coded
   */
  public TablePrinter(List<String> names, boolean truncateOutput, PrintStream out) {
    this.truncateOutput = truncateOutput;
    cols = new Col[names.size()];
    for (int i = 0; i < cols.length; i++) {
      cols[i] = new Col();
      cols[i].name = names.get(i);
      cols[i].maxWidth = getMaxWidth(names.get(i).length());
    }

    rows = new ArrayList<Row>();
    this.out = out;
  }

  private int getMaxWidth(int length) {
    return truncateOutput ? Math.min(MAX_WIDTH, length) : length;
  }

  /**
   * Adds a row - pass in an array or hard coded
   */
  public void addRow(String... values) {
    if (values.length != cols.length) {
      throw new IllegalArgumentException("invalid number of columns in values");
    }

    Row row = new Row(values);
    rows.add(row);
    for (int i = 0; i < values.length; i++) {
      if (values[i].length() > cols[i].maxWidth) {
        cols[i].maxWidth = getMaxWidth(values[i].length());
      }
    }
  }

  /**
   * Helper method to make sure column headers and
   * row information are printed the same
   */
  private void print(String v, int w, PrintStream out) {
    out.print(" ");
    out.print(v);
    out.print(spaces(w - v.length()));
    out.print(" |");
  }

  /**
   * Ugly, poorly documented print method.
   * All pieces of production code should have some
   * methods that you have to decipher. This fulfils that requirement.
   */
  public void print() {
    out.print("|");
    for (Col col : cols) {
      print(col.name, col.maxWidth, out);
    }
    out.println("");
    int numDashes = cols.length * 3 + 1;
    for (Col col : cols) numDashes += col.maxWidth;
    // TODO make columns have + instead of -
    out.println(dashes(numDashes));
    for (Row row : rows) {
      out.print("|");
      int i = 0;
      for (String v : row.data) {
        int maxWidth = cols[i++].maxWidth;
        if (v.length() > maxWidth) {
          v = v.substring(0, maxWidth);
        }
        print(v, maxWidth, out);
      }
      out.println("");
    }
    out.println("");
  }

  // print a specific number of spaces for padding
  private static String spaces(int i) {
    StringBuilder sb = new StringBuilder();
    while (i-- > 0) sb.append(" ");
    return sb.toString();
  }

  // print a specific number of dashes
  private static String dashes(int i) {
    StringBuilder sb = new StringBuilder();
    while (i-- > 0) sb.append("-");
    return sb.toString();
  }
}
