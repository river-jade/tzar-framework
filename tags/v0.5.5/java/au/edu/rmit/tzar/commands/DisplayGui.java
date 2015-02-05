package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.gui.TzarGui;

/**
 * Command to display the Tzar GUI.
 */
public class DisplayGui implements Command {
  @Override
  public boolean execute() throws TzarException {
    // below is so we get correct app name in mac menu. Doesn't work if we do it in the TzarGui constructor!!
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", TzarGui.APP_NAME);
    new TzarGui().display();
    return true;
  }
}
