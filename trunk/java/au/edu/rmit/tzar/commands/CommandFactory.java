package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.repository.CodeSource;
import au.edu.rmit.tzar.resultscopier.*;
import au.edu.rmit.tzar.server.WebServer;
import com.beust.jcommander.JCommander;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.CommandFlags.*;
import static au.edu.rmit.tzar.commands.SharedFlags.*;

/**
 * A factory to create executable command objects. This exists primarily to separate the command line
 * flag parsing logic from the commands themselves.
 */
class CommandFactory {
  private static final Logger LOG = Logger.getLogger(CommandFactory.class.getName());
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
    CodeSource.RepositoryType repositoryType = CREATE_RUNS_FLAGS.getRepositoryType();
    URI projectUri = CREATE_RUNS_FLAGS.getProjectUri();
    File baseModelPath = RUNNER_FLAGS.getBaseModelPath();

    CodeSource modelSource = createCodeSource(revision, repositoryType, projectUri, baseModelPath);

    RunFactory runFactory = new RunFactory(modelSource,
        CREATE_RUNS_FLAGS.getRunset(),"" /* no cluster name for local runs */,
        modelSource.getProjectSpec(baseModelPath));

    File baseOutputPath = new File(RUNNER_FLAGS.getTzarBaseDirectory(), Constants.LOCAL_OUTPUT_DATA_DIR);
    return new ExecLocalRuns(CREATE_RUNS_FLAGS.getNumRuns(), runFactory, baseOutputPath, baseModelPath,
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
    File tzarBaseDirectory = RUNNER_FLAGS.getTzarBaseDirectory();

    Executors.newSingleThreadExecutor().submit(new WebServer(Constants.WEBSERVER_PORT, tzarBaseDirectory));

    File baseRemoteOutputPath = POLL_AND_RUN_FLAGS.getFinalOutputPath();
    ResultsCopier resultsCopier;
    if (baseRemoteOutputPath == null) { // no path specified. don't copy output
      LOG.warning("No final output path specified. Results will be left on this node. Is that intentional?");
      resultsCopier = new NoopCopier();
    } else {
      if (POLL_AND_RUN_FLAGS.getScpOutputHost() != null) {
        SshClientFactory sshClientFactory = new SshClientFactoryKeyAuth(POLL_AND_RUN_FLAGS);
        resultsCopier = new ScpResultsCopier(sshClientFactory, baseRemoteOutputPath);
      } else { // path specified but no host, so use file copier
        resultsCopier = new FileResultsCopier(baseRemoteOutputPath);
      }
    }

    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    RunDao runDao = daoFactory.createRunDao();
    resultsCopier = new CopierFactory().createAsyncCopier(resultsCopier, true, true, runDao, baseRemoteOutputPath);

    File baseLocalOutputPath = new File(tzarBaseDirectory, Constants.POLL_AND_RUN_OUTPUT_DIR);
    return new PollAndRun(runDao, resultsCopier, baseLocalOutputPath, RUNNER_FLAGS.getBaseModelPath(),
        new RunnerFactory());
  }

  public Command newPrintRun() throws TzarException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());
    return new PrintRun(daoFactory.createParametersDao(), PRINT_RUN_FLAGS.getRunId(),
        PRINT_TABLE_FLAGS.isTruncateOutput(), PRINT_TABLE_FLAGS.getOutputType());
  }

  public Command newScheduleRuns() throws IOException, TzarException, ParseException {
    DaoFactory daoFactory = new DaoFactory(getDbUrl());


    CodeSource.RepositoryType repositoryType = CREATE_RUNS_FLAGS.getRepositoryType();
    if (repositoryType == CodeSource.RepositoryType.LOCAL_FILE) {
      throw new ParseException("Repository type: LOCAL_FILE is not valid when scheduling remote runs. " +
          "Please choose a different repository type.");
    }

    CodeSource codeSource = createCodeSource(CREATE_RUNS_FLAGS.getRevision(), repositoryType,
        CREATE_RUNS_FLAGS.getProjectUri(), Files.createTempDir());
    ProjectSpec projectSpec = codeSource.getProjectSpec(Files.createTempDir());

    RunFactory runFactory = new RunFactory(codeSource,
        CREATE_RUNS_FLAGS.getRunset(),
        SCHEDULE_RUNS_FLAGS.getClusterName(),
        projectSpec);
    return new ScheduleRuns(daoFactory.createRunDao(), CREATE_RUNS_FLAGS.getNumRuns(), runFactory);
  }

  private static CodeSource createCodeSource(String revision, CodeSource.RepositoryType repositoryType,
      URI sourceUri, File baseModelPath) throws TzarException {
    if (revision.equals("head")) {
      revision = repositoryType.createRepository(sourceUri, baseModelPath).getHeadRevision();
    } else {
      if (!repositoryType.isValidRevision(revision)) {
        throw new ParseException(revision + " is not a valid revision for repository type: " + repositoryType);
      }
    }

    return new CodeSource(sourceUri, repositoryType, revision);
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
