package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.BriefLogFormatter;
import au.edu.rmit.tzar.ColorConsoleHandler;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ObjectArrays;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main entry point for the tzar framework. Parses command line parameters and pass execution to
 * the appropriate command object.
 */
public class Main {
  private static Logger LOG = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) throws IOException {
    LOG.fine("Starting Tzar with command line args: " + Joiner.on(' ').join(args));
    JCommander jCommander = new JCommander();
    // TODO(river): uncomment this line when we upgrade to newer jcommander. Currently
    // blocked on jcommander help formatting bug: https://github.com/cbeust/jcommander/issues/165
    //jCommander.setAllowAbbreviatedOptions();
    for (CommandFactory.Commands command : CommandFactory.Commands.values()) {
      jCommander.addCommand(command.getName(), ObjectArrays.concat(command.getFlags(), SharedFlags.COMMON_FLAGS));
    }

    jCommander.addObject(SharedFlags.COMMON_FLAGS);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      if (SharedFlags.COMMON_FLAGS.isHelp()) {
        String cmdStr = jCommander.getParsedCommand();
        jCommander.usage(cmdStr);
      } else {
        System.out.println("Try `--help' for more information.");
      }
      System.exit(2);
    }

    if (SharedFlags.COMMON_FLAGS.isHelp()) {
      String cmdStr = jCommander.getParsedCommand();
      if (cmdStr != null) {
        jCommander.usage(cmdStr);
      } else {
        jCommander.usage();
      }
      System.exit(0);
    }

    // We create the default tzar base directory because the logging code expects $HOME/tzar to exist.
    // This is to workaround the following bug:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6244047
    // The FileHandler in the java logging framework barfs if the specified output directory doesn't exist.
    new File(System.getProperty("user.home"), "tzar").mkdir();

    String cmdStr = jCommander.getParsedCommand();
    Optional<CommandFactory.Commands> cmd = CommandFactory.Commands.getCommandByName(cmdStr);

    if (!cmd.isPresent()) {
      if (SharedFlags.COMMON_FLAGS.isVersion()) {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(Main.class.getResourceAsStream("/version.properties")));
        String line;
        while ((line = in.readLine()) != null)
          System.out.println(line);
        return;
      }

      if (cmdStr != null) {
        System.out.println("Command: " + cmdStr + " not recognised.");
      }
      jCommander.usage();
      System.exit(2);
    } else {
      try {
        setupLogging();
        Command command = cmd.get().instantiate(new CommandFactory(jCommander));
        if (!command.execute()) {
          System.exit(1);
        }
      } catch (ParseException e) {
        System.err.println(e.getMessage());
        System.exit(2);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "An unrecoverable error occurred: " + e.getMessage());
        LOG.log(Level.FINE, "An unrecoverable error occurred.", e);
        System.exit(3);
      }
    }
  }

  private static void setupLogging() throws IOException {
    if (System.getProperty("java.util.logging.config.file") == null) {
      LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
    }
    Level level = Level.INFO;
    switch (SharedFlags.COMMON_FLAGS.getLogLevel()) {
      case QUIET:
        level = Level.WARNING;
        break;
      case VERBOSE:
        level = Level.FINE;
        break;
    }
    Handler[] handlers = Logger.getLogger("").getHandlers();
    for (Handler handler : handlers) {
      if (handler instanceof ColorConsoleHandler) {
        handler.setLevel(level);
        ((BriefLogFormatter)handler.getFormatter()).setVerbose(SharedFlags.COMMON_FLAGS.getLogLevel() == SharedFlags
            .CommonFlags.LogLevel.VERBOSE);
      }
    }
  }

}
