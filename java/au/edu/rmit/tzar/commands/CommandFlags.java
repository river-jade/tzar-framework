package au.edu.rmit.tzar.commands;


import au.edu.rmit.tzar.api.Constants;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.common.base.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Command name definitions for the command line parser.
 *
 * @author michaell
 */
public class CommandFlags {
  public static final AggregateResultsFlags AGGREGATE_RESULTS_FLAGS = new AggregateResultsFlags();
  public static final ExecLocalRunsFlags EXEC_LOCAL_RUNS_FLAGS = new ExecLocalRunsFlags();
  public static final HelpFlags HELP_FLAGS = new HelpFlags();
  public static final PollAndRunFlags POLL_AND_RUN_FLAGS = new PollAndRunFlags();
  public static final ScheduleRunsFlags SCHEDULE_RUNS_FLAGS = new ScheduleRunsFlags();
  public static final PrintRunsFlags PRINT_RUNS_FLAGS = new PrintRunsFlags();
  public static final PrintRunFlags PRINT_RUN_FLAGS = new PrintRunFlags();

  /**
   * Command line commandFlags for PollAndRun.
   */
  @Parameters(commandDescription = "Poll the database for scheduled runs and run them one at a " +
      "time.", separators = "= ")
  public static class PollAndRunFlags {
    private PollAndRunFlags() {
    }

    @Parameter(names = "--repository-prefixes", description = "Comma separated list of allowed prefixes " +
        "for repository URIs. If a run whose repository URI does not begin with one of the allowed prefixes is " +
        "retrieved from the database, it will abort and be marked as failed. This flag exists for security reasons. " +
        "Without it, write-access to the database would allow arbitrary code execution on cluster nodes.",
        required=true)
    private List<String> repositoryUriPrefixes;

// TODO(river): reinstate this parameter when concurrent execution works properly
//    @Parameter(names = "--concurrenttaskcount", description = "Number of tasks to run in parallel. In order to set " +
//        "this to above one, the Runner used must support multiple parallel instances.")
    private int concurrentTaskCount = 1;

    @Parameter(names = "--pemfile", description = "Path to ssh private key for connecting to the remote output data " +
        "server. Defaults to $HOME/.ssh/id_rsa", converter = FileConverter.class)
    private File pemFile = new File(System.getProperty("user.home"), ".ssh/id_rsa");

    @Parameter(names = "--runset", description = "Name of runset to poll for. If omitted, will poll for any runs.",
        converter=Converters.OptionalString.class)
    private Optional<String> runset = Optional.absent();

    @Parameter(names = "--scpoutputhost", description = "Hostname for run output data and logs.",
        converter=Converters.OptionalString.class)
    private final Optional<String> scpOutputHost = Optional.absent();

    @Parameter(names = "--finaloutputpath", description = "Remote path for run output data and logs. If " +
        "--scpoutputhost is set, this will be the path on the remote machine that files are copied to. Otherwise, " +
        "files will be copied to this path on the local machine (intended for use with mounted network drives).",
        converter = Converters.OptionalFile.class)
    private final Optional<File> finalOutputPath = Optional.absent();

    @Parameter(names = "--scpoutputuser", description = "Username for the run output ssh host. If not specified, " +
        "uses the current user")
    private final String scpOutputUser = System.getProperty("user.name");

    @Parameter(names = "--rate", description = "Time to wait between database polls (milliseconds).")
    private int pollRateMs = 10000;

    @Parameter(names = "--clustername", description = "Name of the cluster on which this node is running.")
    private String clusterName = Constants.DEFAULT_CLUSTER_NAME;

    public String getClusterName() {
      return clusterName;
    }

    public int getConcurrentTaskCount() {
      return concurrentTaskCount;
    }

    public Optional<File> getFinalOutputPath() {
      return finalOutputPath;
    }

    public List<String> getRepositoryUriPrefixes() {
      return repositoryUriPrefixes;
    }

    public Optional<String> getRunset() {
      return runset;
    }

    public int getPollRateMs() {
      return pollRateMs;
    }

    public Optional<ScpDestination> getScpDestination() {
      if (!scpOutputHost.isPresent()) {
        return Optional.absent();
      }
      if (finalOutputPath.isPresent()) {
        return Optional.of(new ScpDestination(scpOutputHost.get(), scpOutputUser, pemFile));
      } else {
        throw new ParseException("If --scpoutputhost is set, --finaloutputpath must also be set.");
      }
    }
  }

  public static class ScpDestination {
    public final String host;
    public final String scpOutputUser;
    public final File pemFile;

    public ScpDestination(String host, String scpOutputUser, File pemFile) {
      this.host = host;
      this.scpOutputUser = scpOutputUser;
      this.pemFile = pemFile;
    }
  }

  @Parameters(commandDescription = "Execute a set of runs locally.", separators = "= ")
  public static class ExecLocalRunsFlags {
    private ExecLocalRunsFlags() {
    }
  }

  @Parameters(commandDescription = "Schedule the provided set of runs to be executed.", separators = "= ")
  public static class ScheduleRunsFlags {
    private ScheduleRunsFlags() {
    }

    @Parameter(names = "--clustername", description = "Name of cluster to run on.")
    private String clusterName = Constants.DEFAULT_CLUSTER_NAME;

    public String getClusterName() {
      return clusterName;
    }
  }

  @Parameters(commandDescription = "Display help information about the specified command. " +
      "eg <main class> help pollandrun")
  public static class HelpFlags {
    private HelpFlags() {
    }

    @Parameter(description = "Display usage text for the provided command.")
    private List<String> command = new ArrayList<String>();

    public List<String> getCommand() {
      return command;
    }
  }

  @Parameters(commandDescription = "Prints a list of runs matching the provided criteria.", separators = "= ")
  public static class PrintRunsFlags {
    private PrintRunsFlags() {
    }
  }

  @Parameters(commandDescription = "Prints a single run and its parameters.", separators = "= ")
  public static class PrintRunFlags {
    private PrintRunFlags() {
    }

    @Parameter(names = "--runid", description = "Run id.", required = true)
    private Integer runId;

    public Integer getRunId() {
      return runId;
    }
  }

  /**
   * Command line commandFlags for Consolidate Data command.
   */
  @Parameters(commandDescription = "Copy the results of a set of runs to a local dir for analysis.", separators = "= ")
  public static class AggregateResultsFlags {
    @Parameter(names = "--filenamefilter", description = "Regular expression matching name of files to include.",
        required = false, converter=Converters.OptionalString.class)
    private Optional<String> filenameFilter = Optional.absent();

    @Parameter(names = "--outputpath", description = "Local path to put the consolidated data.", required = true)
    private File outputPath;

    @Parameter(names = "--scpusername", description = "Username for the output hosts. If not specified, " +
        "uses the current user")
    private final String scpUserName = System.getProperty("user.name");

    @Parameter(names = "--pemfile", description = "Path to ssh private key for connecting to the remote output data " +
        "server. Defaults to $HOME/.ssh/id_rsa", converter = FileConverter.class)
    private File pemFile = new File(System.getProperty("user.home"), ".ssh/id_rsa");

    @Parameter(names = "--passwordprompt", description = "Prompt for an ssh password for connecting to the remote " +
        "machines")
    private boolean passwordPrompt = false;

    @Parameter(names = "--skipexistingruns", description = "If there are any files already on local disk from a " +
        "given run, skip that run.")
    private boolean skipExistingRuns = false;

    @Parameter(names = "--startrunid", description = "Only copy runs whose run id is equal to or greater than this " +
        "value.")
    private int startRunId = 0;

    private AggregateResultsFlags() {
    }

    public Optional<String> getFilenameFilter() {
      return filenameFilter;
    }

    public File getOutputPath() {
      return outputPath;
    }

    public String getScpUserName() {
      return scpUserName;
    }

    public File getPemFile() {
      return pemFile;
    }

    public boolean isPasswordPrompt() {
      return passwordPrompt;
    }

    public boolean isSkipExistingRuns() { return skipExistingRuns; }

    public int getStartRunId() { return startRunId; }
  }
}
