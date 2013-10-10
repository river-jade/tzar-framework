package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.repository.CodeRepository;
import au.edu.rmit.tzar.repository.SvnRepository;
import au.edu.rmit.tzar.resultscopier.*;
import com.beust.jcommander.JCommander;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileNotFoundException;
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

  public Command newAggregateResults() throws ParseException, TzarException, IOException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new AggregateResults(LOAD_RUNS_FLAGS.getRunIds(), LOAD_RUNS_FLAGS.getStates(),
        LOAD_RUNS_FLAGS.getHostName(), LOAD_RUNS_FLAGS.getRunset(),
        daoFactory.createRunDao(), au.edu.rmit.tzar.Utils.getHostname(), AGGREGATE_RESULTS_FLAGS);
  }

  public Command newExecLocalRuns() throws IOException, TzarException, ParseException {
    String revision = CREATE_RUNS_FLAGS.getRevision();
    RUNNER_FLAGS.getRepositoryType().checkRevisionNumber(revision);

    CodeRepository codeRepository = RUNNER_FLAGS.createRepository();

    RunFactory runFactory = new RunFactory(revision,
        CREATE_RUNS_FLAGS.getRunset(),"" /* no cluster name for local runs */,
        getProjectSpec(codeRepository));

    return new ExecLocalRuns(CREATE_RUNS_FLAGS.getNumRuns(),
        runFactory, RUNNER_FLAGS.getLocalOutputPath(), codeRepository,
        new RunnerFactory());
  }

  public Command newHelp() {
    return new Help(jCommander, HELP_FLAGS.getCommand());
  }

  public Command newPrintRuns() throws TzarException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new PrintRuns(daoFactory.createRunDao(), LOAD_RUNS_FLAGS.getStates(),
        LOAD_RUNS_FLAGS.getHostName(), LOAD_RUNS_FLAGS.getRunset(), LOAD_RUNS_FLAGS.getRunIds(),
        PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
  }

  public Command newPollAndRun() throws IOException, TzarException, ParseException {
    CodeRepository codeRepository = RUNNER_FLAGS.createRepository();

    ResultsCopier resultsCopier;
    if (POLL_AND_RUN_FLAGS.getScpOutputHost() != null) {
      SshClientFactory sshClientFactory = new SshClientFactoryKeyAuth(POLL_AND_RUN_FLAGS);
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

  public Command newPrintRun() throws TzarException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new PrintRun(daoFactory.createParametersDao(), PRINT_RUN_FLAGS.getRunId(),
        PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
  }

  public Command newScheduleRuns() throws IOException, TzarException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    String revision = CREATE_RUNS_FLAGS.getRevision();

    String svnUrl = SCHEDULE_RUNS_FLAGS.getSvnUrl();
    SvnRepository repository = new SvnRepository(svnUrl, null);
    if (SpecialRevisionNumber.CURRENT_HEAD.toString().equalsIgnoreCase(revision)) {
      if (Strings.isNullOrEmpty(svnUrl)) {
        throw new ParseException("--svnurl must not be empty if --revision=current_head");
      }
      revision = Long.toString(repository.getHeadRevision());
    } else if (SpecialRevisionNumber.RUNTIME_HEAD.toString().equalsIgnoreCase(revision)) {
      revision = "head";
    }

    RunnerFlags.RepositoryType.SVN.checkRevisionNumber(revision);

    ProjectSpec projectSpec = getProjectSpec(repository);

    RunFactory runFactory = new RunFactory(revision,
        CREATE_RUNS_FLAGS.getRunset(),
        SCHEDULE_RUNS_FLAGS.getClusterName(),
        projectSpec);
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

  private static ProjectSpec getProjectSpec(CodeRepository codeRepository) throws TzarException {
    YamlParser parser = new YamlParser();
    File projectSpec;
    if (CREATE_RUNS_FLAGS.getProjectSpec() == null) {
      projectSpec = codeRepository.getProjectParams("projectparams.yaml",
          CREATE_RUNS_FLAGS.getRevision());
    } else {
      projectSpec = CREATE_RUNS_FLAGS.getProjectSpec();
    }
    try {
      return parser.projectSpecFromYaml(projectSpec);
    } catch (FileNotFoundException e) {
      throw new TzarException("Couldn't parse project spec at: " + projectSpec, e);
    }
  }

  /**
   * Enumeration of all possible commands supported by the CLI.
   */
  static enum Commands {
    AGGREGATE_RESULTS("aggregate", AggregateResults.FLAGS) {
      @Override
      public Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
        return factory.newAggregateResults();
      }
    },
    EXEC_LOCAL_RUNS("execlocalruns", ExecLocalRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
        return factory.newExecLocalRuns();
      }
    },
    HELP("help", Help.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException {
        return factory.newHelp();
      }
    },
    POLL_AND_RUN("pollandrun", PollAndRun.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
        return factory.newPollAndRun();
      }
    },
    PRINT_RUNS("printruns", PrintRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
        return factory.newPrintRuns();
      }
    },
    PRINT_RUN("printrun", PrintRun.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
        return factory.newPrintRun();
      }
    },
    SCHEDULE_RUNS("scheduleruns", ScheduleRuns.FLAGS) {
      @Override
      Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException {
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

    abstract Command instantiate(CommandFactory factory) throws IOException, TzarException, ParseException;

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
