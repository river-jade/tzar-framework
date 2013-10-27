package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.TzarException;

/**
 * Represents a command to be run by the Main class.
 */
public interface Command {
  /**
   * Executes this command.
   *
   * @return true if the command  succeeded and false otherwise
   * @throws InterruptedException if the thread is interrupted while blocking
   * @throws TzarException        if another non-critical error occurs
   */
  boolean execute() throws InterruptedException, TzarException;
}
