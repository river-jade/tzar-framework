package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.RdvException;

/**
 * Represents a command to be run by the Main class.
 */
public interface Command {
  /**
   * Executes this command.
   *
   * @return true if the command succeeded and false otherwise
   * @throws InterruptedException if the thread is interrupted while blocking
   * @throws RdvException         if another non-critical error occurs
   */
  boolean execute() throws InterruptedException, RdvException;
}
