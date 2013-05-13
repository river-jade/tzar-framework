package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.JCommander;

import java.util.List;

/**
 * Command to display help on usage of the other commands.
 */
class Help implements Command {
  public final static Object[] FLAGS = new Object[]{CommandFlags.HELP_FLAGS};

  private final JCommander jCommander;
  private List<String> commandList;

  public Help(JCommander jCommander, List<String> commandList) {
    this.jCommander = jCommander;
    this.commandList = commandList;
  }

  @Override
  public boolean execute() throws InterruptedException, TzarException {
    if (!commandList.isEmpty()) {
      jCommander.usage(commandList.get(0));
    } else {
      jCommander.usage();
    }
    return true;
  }

}
