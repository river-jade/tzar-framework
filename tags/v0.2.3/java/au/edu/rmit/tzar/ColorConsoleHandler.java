package au.edu.rmit.tzar;

import org.fusesource.jansi.AnsiConsole;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * A console log handler which can write colour output.
 */
public class ColorConsoleHandler extends StreamHandler {
  public ColorConsoleHandler() {
    super(AnsiConsole.err, new BriefLogFormatter());
  }

  @Override
  public void publish(LogRecord record) {
    super.publish(record);
    flush();
  }

  @Override
  public void close() throws SecurityException {
    super.flush();
  }
}
