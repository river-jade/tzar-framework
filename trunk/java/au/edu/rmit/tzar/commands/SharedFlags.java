package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.Constants;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.db.Utils;
import au.edu.rmit.tzar.repository.CodeRepository;
import au.edu.rmit.tzar.repository.LocalFileRepository;
import au.edu.rmit.tzar.repository.SvnRepository;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line commandFlags which are common for all commands.
 */
@com.beust.jcommander.Parameters(separators = "= ")
class SharedFlags {
  private SharedFlags() {
  }

  public static final CreateRunsFlags CREATE_RUNS_FLAGS = new CreateRunsFlags();
  public static final DbFlags DB_FLAGS = new DbFlags();
  public static final LoadRunsFlags LOAD_RUNS_FLAGS = new LoadRunsFlags();
  public static final RunnerFlags RUNNER_FLAGS = new RunnerFlags();
  public static final PrintTableFlags PRINT_TABLE_FLAGS = new PrintTableFlags();
  public static final CommonFlags COMMON_FLAGS = new CommonFlags();

  @Parameters(separators = "= ")
  public static class RunnerFlags {
    private RunnerFlags() {
    }

    @Parameter(names = "--localcodepath", description = "Path for local code repository to use " +
        "instead of SVN.", converter = FileConverter.class)
    private File localCodePath = null;

    @Parameter(names = "--svnurl", description = "URL for the SVN repository.")
    private String svnUrl = null;

    @Parameter(names = "--tzarbasedir", description = "Base directory to store tzar files, including temporary " +
        "output files, and downloaded source code.",
        converter = FileConverter.class)
    private File tzarBaseDirectory = new File(System.getProperty("user.home"), "tzar");

    public CodeRepository createRepository() {
      return getRepositoryType().createRepository(this);
    }

    public RepositoryType getRepositoryType() {
      if (localCodePath == null ^ svnUrl != null) {
        throw new ParseException("Exactly one of localcodepath or svnurl must be provided");
      } else if (localCodePath != null) {
        return RepositoryType.LOCAL;
      } else { // (svnUrl != null)
        return RepositoryType.SVN;
      }
    }

    public File getBaseModelsPath() {
      return new File(tzarBaseDirectory, "modelcode");
    }

    public File getLocalOutputPath() {
      return new File(tzarBaseDirectory, "outputdata");
    }

    public enum RepositoryType {
      SVN {
        @Override
        public void checkRevisionNumber(String revision) {
          if (revision == null || revision.length() == 0) {
            throw new ParseException("You must specify a revision using --revision when using SVN repository.");
          }
          try {
            // check that revision number is valid
            SvnRepository.parseSvnRevision(revision);
          } catch (RdvException e) {
            throw new ParseException(e);
          }
        }

        @Override
        CodeRepository createRepository(RunnerFlags flags) {
          return new SvnRepository(flags.svnUrl, flags.getBaseModelsPath());
        }
      },
      LOCAL {
        @Override
        public CodeRepository createRepository(RunnerFlags flags) {
          return new LocalFileRepository(flags.localCodePath);
        }

        @Override
        public void checkRevisionNumber(String revision) {
          if (revision != null && revision.length() != 0) {
            throw new ParseException("--revision may only be used for svn repositories");
          }
        }
      };

      abstract CodeRepository createRepository(RunnerFlags flags);

      public abstract void checkRevisionNumber(String revision);
    }
  }

  public enum SpecialRevisionNumber {
    CURRENT_HEAD,
    RUNTIME_HEAD;
  }

  @Parameters(separators = "= ")
  public static class DbFlags {
    private DbFlags() {
    }

    @com.beust.jcommander.Parameter(names = "--dburl", description = "The jdbc access URL for the database.")
    private String dbUrl = System.getenv(Constants.DB_ENVIRONMENT_VARIABLE_NAME);

    public String getDbUrl() {
      return dbUrl;
    }
  }

  @Parameters(separators = "= ")
  public static class CreateRunsFlags {
    private CreateRunsFlags() {
    }

    @Parameter(names = "--commandlineflags", description = "Comma-separated list of command line flags to pass to " +
        "the runner when executing this run")
    private String commandFlags = "";

    @Parameter(names = "--globalparams", description = "Path to a file containing the a set of params to be " +
        "overridden by the project spec. This is used for parameters which are shared across multiple projects and " +
        "is optional. This is ignored if --runspec is set.")
    private File globalParams = null;

    @Parameter(names = {"-n", "--numruns"}, description = "Number of runs to schedule")
    private int numRuns = 1;

    @Parameter(names = "--projectspec", description = "The path to the file containing the project spec. Either this " +
        "or --runspec must be set.")
    private File projectSpec = null;

    @Parameter(names = "--runnerclass", description = "Fully qualified classname of the runner class, or name of a " +
        "class in the au.edu.rmit.tzar.runners package.", required = true)
    private String runnerClass;

    @Parameter(names = "--runspec", description = "Path to the file containing a single parameter set, " +
        "ie for a single " +
        "run. Either this or projectSpec must be set.")
    private File runSpec = null;

    // TODO(michaell): use this or remove it
    @Parameter(names = "--projectpath", description = "Not yet implemented. The relative path to the model in the " +
        "repository")
    private File projectPath = null;

    @Parameter(names = "--repetitionsfile", description = "Filename containing repetition definitions.")
    private File repetitionsPath = null;

    @Parameter(names = "--runset", description = "Name of runset to schedule.")
    private String runset = "";

    @Parameter(names = "--clustername", description = "Name of cluster to run on.")
    private String clusterName = "";

    @Parameter(names = "--revision", description = "The source control revision of the model code to schedule for " +
        "execution. Must be either an integer, 'runtime_head', or 'current_head'. 'runtime_head' will mean that " +
        "clients will always download the latest version of the code, 'current_head' will set the revision to be " +
        "the head revision at the time the job is scheduled. This flag is mandatory if --svnurl is set. Conversely, " +
        "--svnurl is mandatory if the value of this flag is 'current_head'.")
    private String revision = null;

    public String getCommandFlags() {
      return commandFlags;
    }

    public File getGlobalParamsPath() {
      return globalParams;
    }

    public int getNumRuns() {
      return numRuns;
    }

    public File getProjectSpec() {
      return projectSpec;
    }

    // TODO(michaell): implement this
    public File getProjectPath() {
      return projectPath;
    }

    public File getRepetitionsPath() {
      return repetitionsPath;
    }

    public String getRunnerClass() {
      return runnerClass;
    }

    public String getRunset() {
      return runset;
    }

    public String getRevision() {

      return revision;
    }

    public File getRunSpec() {
      return runSpec;
    }

    public String getClusterName() {
      return clusterName;
    }
  }

  /**
   * Flags for command which load a set of runs matching some criteria.
   */
  @Parameters(separators = "= ")
  public static class LoadRunsFlags {
    @Parameter(names = "--states", description = "Run states to filter by.")
    private List<String> states = new ArrayList<String>();

    @Parameter(names = "--hostname", description = "Host name to filter by.")
    private String hostName = null;

    @Parameter(names = "--runset", description = "Runset name to filter by.")
    private String runset = null;

    @Parameter(names = "--runids", description = "List of run ids.")
    private List<Integer> runIds = null;

    public List<String> getStates() {
      return states;
    }

    public String getHostName() {
      return hostName;
    }

    public String getRunset() {
      return runset;
    }

    public List<Integer> getRunIds() {
      return runIds;
    }
  }

  /**
   * Flags for commands which print tables.
   */
  @Parameters(separators = "= ")
  public static class PrintTableFlags {
    private PrintTableFlags() {
    }

    @Parameter(names = "--csv", description = "Set if output should be CSV format.")
    private boolean outputType;

    @Parameter(names = "--notruncate", description = "Set if output fields should be arbitrarily long.")
    private boolean noTruncateOutput = false;

    public boolean isTruncateOutput() {
      return !noTruncateOutput;
    }

    public Utils.OutputType getOutputType() {
      return outputType ? Utils.OutputType.CSV : Utils.OutputType.PRETTY;
    }
  }

  public static class CommonFlags {
    private CommonFlags() {
    }

    @Parameter(names = {"-v", "--verbose"}, description = "Verbose logging to console.")
    private boolean verbose = false;

    @Parameter(names = {"-q", "--quiet"}, description = "Quiet logging to console.")
    private boolean quiet = false;

    @Parameter(names = {"--help"}, description = "Show help info.")
    private boolean help = false;

    @Parameter(names = {"--version"}, description = "Show version info.")
    private boolean version = false;

    public LogLevel getLogLevel() throws ParseException {
      if (verbose && quiet) {
        throw new ParseException("Can not specify both --verbose and --quiet.");
      } else if (verbose) {
        return LogLevel.VERBOSE;
      } else if (quiet) {
        return LogLevel.QUIET;
      } else {
        return LogLevel.NORMAL;
      }
    }

    public boolean isHelp() {
      return help;
    }

    public boolean isVersion() {
      return version;
    }

    public enum LogLevel {
      QUIET,
      NORMAL,
      VERBOSE
    }
  }
}
