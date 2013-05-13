package au.edu.rmit.tzar.api;

/**
 * A wrapper exception class for recoverable errors thrown in the framework code.
 */
public class TzarException extends Exception {
  public TzarException(String message, Throwable cause) {
    super(message, cause);
  }

  public TzarException(String message) {
    super(message);
  }

  public TzarException(Throwable cause) {
    super(cause);
  }
}
