package au.edu.rmit.tzar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BriefLogFormatter extends Formatter {

  private static final DateFormat format = new SimpleDateFormat("h:mm:ss");
  private static final String lineSep = System.getProperty("line.separator");

  /**
   * A Custom format implementation that is designed for brevity.
   */
  public String format(LogRecord record) {
    String loggerName = record.getLoggerName();
    if (loggerName == null) {
      loggerName = "root";
    }
    StringBuilder output = new StringBuilder()
        .append("[")
        .append(record.getLevel()).append('|')
        .append(format.format(new Date(record.getMillis())))
        .append("]: ")
        .append(record.getMessage()).append(' ')
        .append(lineSep);
    if (record.getThrown() != null) {
      try {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        output.append(sw.toString());
      } catch (Exception ex) {
      }
    }
    return output.toString();
  }

}
