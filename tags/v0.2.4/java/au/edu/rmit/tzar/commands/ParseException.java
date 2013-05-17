package au.edu.rmit.tzar.commands;

import com.beust.jcommander.ParameterException;

/**
 * Unchecked Exception to indicate that there was an issue validating command line
 * parameters.
 */
class ParseException extends ParameterException {
  public ParseException(Throwable t) {
    super(t);
  }

  public ParseException(String errorMessage) {
    super(errorMessage);
  }
}
