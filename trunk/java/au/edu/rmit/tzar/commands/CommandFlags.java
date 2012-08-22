package au.edu.rmit.tzar.commands;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

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

    @Parameter(names = "--concurrenttaskcount", description = "Number of tasks to run in parallel. In order to set " +
        "this to above one, the Runner used must support multiple parallel instances.")
    private int concurrentTaskCount = 1;

    @Parameter(names = "--pemfile", description = "Path to ssh private key for connecting to the remote output data " +
        "server. Defaults to $HOME/.ssh/id_rsa", converter = FileConverter.class)
    private File pemFile = null;

    @Parameter(names = "--runset", description = "Name of runset to poll for. If omitted, will poll for any runs.")
    private String runset = null;

    @Parameter(names = "--scpoutputhost", description = "Hostname for run output data and logs.")
    private final String scpOutputHost = null;

    @Parameter(names = "--scpoutputpath", description = "Remote path for run output data and logs. This will be " +
        "used for final location of output files if --scpoutputhost is set.",
        converter = FileConverter.class)
    private final File scpOutputPath = new File("tzar/outputdata");

    @Parameter(names = "--sleeptime", description = "Time to wait between database polls (millis).")
    private int sleepTimeMillis = 10000;

    public int getConcurrentTaskCount() {
      return concurrentTaskCount;
    }

    public String getRunset() {
      return runset;
    }

    public int getSleepTimeMillis() {
      return sleepTimeMillis;
    }

    public String getScpOutputHost() {
      return scpOutputHost;
    }

    public File getScpOutputPath() {
      return scpOutputPath;
    }

    public File getPemFile() {
      return pemFile;
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
    private Integer runId = null;

    public Integer getRunId() {
      return runId;
    }
  }

  /**
   * Command line commandFlags for Consolidate Data command.
   */
  @Parameters(commandDescription = "Copy the results of a set of runs to a local dir for analysis.", separators = "= ")
  public static class AggregateResultsFlags {
    @Parameter(names = "--outputpath", description = "Local path to put the consolidated data.", required = true)
    private File outputPath;

    private AggregateResultsFlags() {
    }

    public File getOutputPath() {
      return outputPath;
    }
  }
}
