package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.SSHClientFactory;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.repository.CodeRepository;
import au.edu.rmit.tzar.resultscopier.CopierFactory;
import au.edu.rmit.tzar.resultscopier.FileResultsCopier;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;
import au.edu.rmit.tzar.resultscopier.ScpResultsCopier;
import com.beust.jcommander.JCommander;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;

import static au.edu.rmit.tzar.commands.CommandFlags.*;
import static au.edu.rmit.tzar.commands.SharedFlags.*;

/**
 * A factory to create executable command objects. This exists primarily to separate the command line
 * flag parsing logic from the commands themselves.
 */
class CommandFactory {
  private final JCommander jCommander;

  public CommandFactory(JCommander jCommander) {
    this.jCommander = jCommander;
  }

  public Command newAggregateResults() throws ParseException, RdvException, IOException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new AggregateResults(LOAD_RUNS_FLAGS.getRunIds(), LOAD_RUNS_FLAGS.getStates(),
        LOAD_RUNS_FLAGS.getHostName(), LOAD_RUNS_FLAGS.getRunset(), AGGREGATE_RESULTS_FLAGS.getOutputPath(),
        daoFactory.createRunDao(), au.edu.rmit.tzar.Utils.getHostname(), AGGREGATE_RESULTS_FLAGS.getScpUserName(),
        AGGREGATE_RESULTS_FLAGS.getFilenameFilter(), AGGREGATE_RESULTS_FLAGS.getPemFile());
  }

  public Command newExecLocalRuns() throws IOException, RdvException, ParseException {
    if (CREATE_RUNS_FLAGS.getProjectSpec() == null ^ CREATE_RUNS_FLAGS.getRunSpec() != null) {
      throw new ParseException("Must set exactly one of --projectspec or --runspec.");
    }

    String revision = CREATE_RUNS_FLAGS.getRevision();
    RUNNER_FLAGS.getRepositoryType().checkRevisionNumber(revision);

    YamlParser parser = new YamlParser();

    RunFactory runFactory = new RunFactory(revision,
        CREATE_RUNS_FLAGS.getCommandFlags(), CREATE_RUNS_FLAGS.getRunset(),
        CREATE_RUNS_FLAGS.getClusterName(), parser.projectSpecFromYaml(CREATE_RUNS_FLAGS.getProjectSpec()),
        parser.repetitionsFromYaml(CREATE_RUNS_FLAGS.getRepetitionsPath()),
        parser.parametersFromYaml(CREATE_RUNS_FLAGS.getGlobalParamsPath()));

    CodeRepository codeRepository = RUNNER_FLAGS.createRepository();
    return new ExecLocalRuns(CREATE_RUNS_FLAGS.getNumRuns(),
        runFactory, RUNNER_FLAGS.getLocalOutputPath(), codeRepository,
        new RunnerFactory(), CREATE_RUNS_FLAGS.getRunnerClass());
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
    CodeRepository codeRepository = RUNNER_FLAGS.createRepository();

    ResultsCopier resultsCopier;
    if (POLL_AND_RUN_FLAGS.getScpOutputHost() != null) {
      SSHClientFactory sshClientFactory = new SSHClientFactory(POLL_AND_RUN_FLAGS);
      resultsCopier = new ScpResultsCopier(sshClientFactory, POLL_AND_RUN_FLAGS.getScpOutputPath());
    } else {
      resultsCopier = new FileResultsCopier(RUNNER_FLAGS.getLocalOutputPath());
    }

    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    RunDao runDao = daoFactory.createRunDao();
    resultsCopier = new CopierFactory().createAsyncCopier(resultsCopier, true, true, runDao);

    return new PollAndRun(runDao, resultsCopier, RUNNER_FLAGS.getLocalOutputPath(), codeRepository,
        new RunnerFactory());
  }

  public Command newPrintRun() throws RdvException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new PrintRun(daoFactory.createParametersDao(), PRINT_RUN_FLAGS.getRunId(),
        PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
  }

  public Command newScheduleRuns() throws IOException, RdvException, ParseException {
    if ((CREATE_RUNS_FLAGS.getProjectSpec() == null) == (CREATE_RUNS_FLAGS.getRunSpec() == null)) {
      throw new ParseException("Must set exactly one of --projectspec or --runspec.");
    }

    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    String revision = CREATE_RUNS_FLAGS.getRevision();
    RunnerFlags.RepositoryType.SVN.checkRevisionNumber(revision);

    YamlParser parser = new YamlParser();

    RunFactory runFactory = new RunFactory(revision,
        CREATE_RUNS_FLAGS.getCommandFlags(), CREATE_RUNS_FLAGS.getRunset(),
        CREATE_RUNS_FLAGS.getClusterName(),
        parser.projectSpecFromYaml(CREATE_RUNS_FLAGS.getProjectSpec()),
        parser.repetitionsFromYaml(CREATE_RUNS_FLAGS.getRepetitionsPath()),
        parser.parametersFromYaml(CREATE_RUNS_FLAGS.getGlobalParamsPath()));
    return new ScheduleRuns(daoFactory.createRunDao(), CREATE_RUNS_FLAGS.getNumRuns(), runFactory,
        CREATE_RUNS_FLAGS.getRunnerClass());
  }

  private String getDbUrl() throws ParseException {
    String dbString = DB_FLAGS.getDbUrl();
    if (dbString == null) {
      throw new ParseException("The database string must be set as the environment variable: " +
          Constants.DB_ENVIRONMENT_VARIABLE_NAME + " or by the flag --dburl");
    }
    return dbString;
  }

  /**
   * Enumeration of all possible commands supported by the CLI.
   */
  static enum Commands {
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

    public static Commands getCommandByName(String name) {
      return map.get(name);
    }

    public String getName() {
      return name;
    }

    public Object[] getFlags() {
      return flags;
    }
    
    private final Object[] flags;
  }
}
