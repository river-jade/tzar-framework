package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.gui.TzarGui;

/**
 * Command to display the Tzar GUI.
 */
public class DisplayGui implements Command {
  @Override
  public boolean execute() throws TzarException {
    new TzarGui().display();
    return true;
  }
}
