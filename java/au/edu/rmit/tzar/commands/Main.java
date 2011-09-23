package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.parser.JsonParser;
import au.edu.rmit.tzar.repository.CodeRepository;
import au.edu.rmit.tzar.resultscopier.FileResultsCopier;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;
import au.edu.rmit.tzar.resultscopier.ScpResultsCopier;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import net.schmizz.sshj.SSHClient;

import java.io.IOException;
import java.util.Map;
import java.util.logging.*;

import static au.edu.rmit.tzar.commands.CommandFlags.*;
import static au.edu.rmit.tzar.commands.SharedFlags.*;

/**
 * Main entry point for the tzar framework. Parses command line parameters and pass execution to
 * the appropriate command object.
 */
public class Main {
  public static void main(String[] args) throws IOException, RdvException, InterruptedException {
    JCommander jCommander = new JCommander();
    for (Commands command : Commands.values()) {
      jCommander.addCommand(command.name, ObjectArrays.concat(command.flags, SharedFlags.COMMON_FLAGS));
    }

    try {
      jCommander.parse(args);
      setupLogging();

      String cmdStr = jCommander.getParsedCommand();

      Commands cmd = Commands.map.get(cmdStr);
      if (cmd == null) {
        if (cmdStr != null) {
          System.out.println("Command: " + cmdStr + " not recognised.");
        }
        jCommander.usage();
        System.exit(2);
      } else {
        Command command = cmd.instantiate(new CommandFactory(jCommander));
        if (!command.execute()) {
          System.exit(1);
        }
      }
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      String cmdStr = jCommander.getParsedCommand();
      if (cmdStr != null) {
        jCommander.usage(cmdStr);
      } else {
        jCommander.usage();
      }
      System.exit(2);
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
      if (handler instanceof ConsoleHandler) {
        handler.setLevel(level);
      }
    }
  }

  /**
   * Enumeration of all possible commands supported by the CLI.
   */
  private static enum Commands {
    AGGREGATE_RESULTS("aggregate", AggregateResults.FLAGS) {
      @Override
      public Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newAggregateResults();
      }
    },
    EXEC_LOCAL_RUNS("execlocalruns", ExecLocalRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newExecLocalRuns();
      }
    },
    HELP("help", Help.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException {
        return factory.newHelp();
      }
    },
    POLL_AND_RUN("pollandrun", PollAndRun.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newPollAndRun();
      }
    },
    PRINT_RUNS("printruns", PrintRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newPrintRuns();
      }
    },
    PRINT_RUN("printrun", PrintRun.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newPrintRun();
      }
    },
    SCHEDULE_RUNS("scheduleruns", ScheduleRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException {
        return factory.newScheduleRuns();
      }
    };

    private final String name;
    private final Object[] flags;
    private static final Map<String, Commands> map = Maps.newHashMap();

    static {
      for (Commands command : Commands.values()) {
        map.put(command.name, command);
      }
    }

    /**
     * Enum constructor.
     *
     * @param name  The name of this command, as represented on the command line, and in help.
     * @param flags The list of objects annotated with flags supported by this command. The first of
     *              these should be annotated as a command.
     */
    Commands(String name, Object[] flags) {
      this.name = name;
      this.flags = flags;
    }

    abstract Command instantiate(CommandFactory factory) throws IOException, RdvException, ParseException;
  }

  /**
   * A factory to create executable command objects. This exists primarily to separate the command line
   * flag parsing logic from the commands themselves.
   */
  private static class CommandFactory {
    private final JCommander jCommander;

    public CommandFactory(JCommander jCommander) {
      this.jCommander = jCommander;
    }

    public Command newAggregateResults() throws ParseException, RdvException, IOException {
      DaoFactory daoFactory = new DaoFactory(getDbUrl());
      return new AggregateResults(LOAD_RUNS_FLAGS.getRunIds(), LOAD_RUNS_FLAGS.getStates(),
          LOAD_RUNS_FLAGS.getHostName(), LOAD_RUNS_FLAGS.getRunset(), AGGREGATE_RESULTS_FLAGS.getOutputPath(),
          daoFactory.createRunDao(), au.edu.rmit.tzar.Utils.getHostname());
    }

    public Command newExecLocalRuns() throws IOException, RdvException, ParseException {
      if (CREATE_RUNS_FLAGS.getProjectSpec() == null &&
          CREATE_RUNS_FLAGS.getRunSpec() == null) {
        throw new ParseException("Must set either --projectspec or --runspec.");
      }
      if (!(RUNNER_FLAGS.getLocalCodePath() == null ^ RUNNER_FLAGS.getSvnUrl() == null)) {
        throw new ParseException("Must set exactly one of --localcodepath or --svnurl.");
      }

      JsonParser jsonParser = new JsonParser();
      RunFactory runFactory = new RunFactory(jsonParser, CREATE_RUNS_FLAGS.getRevision(),
          CREATE_RUNS_FLAGS.getCommandFlags(), CREATE_RUNS_FLAGS.getRunset(),
          CREATE_RUNS_FLAGS.getProjectSpec(), CREATE_RUNS_FLAGS.getRepetitionsPath(),
          CREATE_RUNS_FLAGS.getGlobalParamsPath());
      CodeRepository codeRepository = Utils.createCodeRepository(RUNNER_FLAGS.getLocalCodePath(),
          RUNNER_FLAGS.getBaseModelsPath(), RUNNER_FLAGS.getSvnUrl());

      return new ExecLocalRuns(CREATE_RUNS_FLAGS.getRunSpec(), CREATE_RUNS_FLAGS.getNumRuns(),
          runFactory, RUNNER_FLAGS.getLocalOutputPath(), codeRepository, new RunnerFactory());
    }

    public Command newHelp() {
      return new Help(jCommander, HELP_FLAGS.getCommand());
    }

    public Command newPrintRuns() throws RdvException, ParseException {
      DaoFactory daoFactory = new DaoFactory(getDbUrl());
      return new PrintRuns(daoFactory.createRunDao(), LOAD_RUNS_FLAGS.getStates(),
          LOAD_RUNS_FLAGS.getHostName(), LOAD_RUNS_FLAGS.getRunset(), LOAD_RUNS_FLAGS.getRunIds(),
          PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
    }

    public Command newPollAndRun() throws IOException, RdvException, ParseException {
      DaoFactory daoFactory = new DaoFactory(getDbUrl());
      if (RUNNER_FLAGS.getLocalCodePath() == null && RUNNER_FLAGS.getSvnUrl() == null) {
        throw new ParseException("Must set either --localcodepath or --svnurl.");
      }

      CodeRepository codeRepository = Utils.createCodeRepository(RUNNER_FLAGS.getLocalCodePath(),
          RUNNER_FLAGS.getBaseModelsPath(), RUNNER_FLAGS.getSvnUrl());

      ResultsCopier resultsCopier;
      if (POLL_AND_RUN_FLAGS.getScpOutputHost() != null) {
        SSHClient sshClient = Utils.createSSHClient(POLL_AND_RUN_FLAGS.getScpOutputHost());
        resultsCopier = new ScpResultsCopier(POLL_AND_RUN_FLAGS.getScpOutputHost(),
            POLL_AND_RUN_FLAGS.getScpOutputPath(), sshClient);
      } else {
        resultsCopier = new FileResultsCopier(RUNNER_FLAGS.getLocalOutputPath());
      }

      return new PollAndRun(daoFactory, POLL_AND_RUN_FLAGS.getSleepTimeMillis(), resultsCopier,
          POLL_AND_RUN_FLAGS.getRunset(), POLL_AND_RUN_FLAGS.getConcurrentTaskCount(),
          RUNNER_FLAGS.getLocalOutputPath(), codeRepository, new RunnerFactory());
    }

    public Command newPrintRun() throws RdvException, ParseException {
      DaoFactory daoFactory = new DaoFactory(getDbUrl());
      return new PrintRun(daoFactory.createParametersDao(), PRINT_RUN_FLAGS.getRunId(),
          PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
    }

    public Command newScheduleRuns() throws IOException, RdvException, ParseException {
      DaoFactory daoFactory = new DaoFactory(getDbUrl());
      RunFactory runFactory = new RunFactory(new JsonParser(), CREATE_RUNS_FLAGS.getRevision(),
          CREATE_RUNS_FLAGS.getCommandFlags(), CREATE_RUNS_FLAGS.getRunset(), CREATE_RUNS_FLAGS.getProjectSpec(),
          CREATE_RUNS_FLAGS.getRepetitionsPath(), CREATE_RUNS_FLAGS.getGlobalParamsPath());
      return new ScheduleRuns(daoFactory.createRunDao(), CREATE_RUNS_FLAGS.getNumRuns(), runFactory);
    }

    private String getDbUrl() throws ParseException {
      String dbString = DB_FLAGS.getDbUrl();
      if (dbString == null) {
        throw new ParseException("The database string must be set as the environment variable: " +
            Constants.DB_ENVIRONMENT_VARIABLE_NAME + " or by the flag --dburl");
      }
      return dbString;
    }
  }

  static class ParseException extends ParameterException {
    public ParseException(String errorMessage) {
      super(errorMessage);
    }
  }
}
