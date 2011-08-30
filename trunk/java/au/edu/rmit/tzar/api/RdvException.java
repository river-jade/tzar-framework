package au.edu.rmit.tzar.api;

/**
 * A wrapper exception class for recoverable errors thrown in the framework code.
 */
public class RdvException extends Exception {
  public RdvException(String message, Throwable cause) {
    super(message, cause);
  }

  public RdvException(String message) {
    super(message);
  }

  public RdvException(Throwable cause) {
    super(cause);
  }
}
