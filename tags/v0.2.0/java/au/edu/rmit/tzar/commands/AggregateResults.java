package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.SSHClientFactory;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.io.Files;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.LOAD_RUNS_FLAGS;

/**
 * Copy a set of results to a single location for analysis, renaming and flattening
 * the files for ease of analysis and to avoid clashes.
 */
class AggregateResults implements Command {
  public static final Object[] FLAGS = new Object[]{CommandFlags.AGGREGATE_RESULTS_FLAGS, DB_FLAGS, LOAD_RUNS_FLAGS};
  private static final Logger LOG = Logger.getLogger(AggregateResults.class.getName());

  private final List<Integer> runIds;
  private final RunDao runDao;
  private final String runset;
  private final File destPath;
  private final List<String> states;
  /**
   * Host name (of machine on which run was executed) to filter results by
   */
  private final String filterHostname;
  /**
   * Hostname of this machine
   */
  private final String hostname;
  /**
   * Collection of SSH connections, keyed by server name to avoid duplicating connections
   * or having to reconnect each time.
   */
  private final Map<String, SSHClient> connections;

  /**
   * Constructor.
   *
   * @param runIds         list of ids of runs to query. Empty or null to not filter by id.
   * @param states         list of states of runs to query. Empty or null to default to 'copied'.
   * @param filterHostname host name (of machine on which run was executed) to filter results by
   * @param runset         name of runset to filter by, or null to not filter by runset
   * @param destPath       base path to copy aggregated results to
   * @param runDao         for accessing the database of runs
   * @param hostname       host name of this machine (used to determing if ssh is required to copy files)
   */
  public AggregateResults(List<Integer> runIds, List<String> states, String filterHostname, String runset,
      File destPath, RunDao runDao, String hostname) {
    this.runIds = runIds;
    if (states == null || states.isEmpty()) {
      this.states = Lists.newArrayList("copied");
    } else {
      this.states = states;
    }
    this.filterHostname = filterHostname;
    this.runset = runset;
    this.destPath = destPath;
    this.runDao = runDao;
    this.hostname = hostname;
    connections = new MapMaker().makeComputingMap(new Function<String, SSHClient>() {
      @Override
      public SSHClient apply(String sourceHost) {
        try {
          return new SSHClientFactory(sourceHost, null).createSSHClient();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  public boolean execute() throws RdvException {
    List<Run> runs = runDao.getRuns(states, filterHostname, runset, runIds);
    try {
      for (Run run : runs) {
        LOG.info("Copying results for run: " + run);
        String sourceHost = run.getOutputHost();
        File runOutputPath = run.getOutputPath();
        if (hostname.equals(sourceHost)) {
          LOG.info("Results are on localhost. Using copy to copy results.");
          au.edu.rmit.tzar.Utils.copyDirectory(runOutputPath, destPath, new RunIdRenamer(run.getRunId()));
        } else {
          LOG.info("Results are on machine: " + sourceHost + ". Using ssh to copy results.");
          SCPFileTransfer scpFileTransfer = connections.get(sourceHost).newSCPFileTransfer();
          File tempPath = Files.createTempDir();
          scpFileTransfer.download(runOutputPath.getPath(), tempPath.getPath());
          au.edu.rmit.tzar.Utils.copyDirectory(new File(tempPath, runOutputPath.getName()),
              destPath, new RunIdRenamer(run.getRunId()));
        }
      }
    } catch (IOException e) {
      throw new RdvException(e);
    } finally {
      for (SSHClient connection : connections.values()) {
        try {
          connection.disconnect();
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Error closing SSH connection.", e);
        }
      }
    }
    return true;
  }

  public class RunIdRenamer implements au.edu.rmit.tzar.Utils.RenamingStrategy {
    private final int runId;

    public RunIdRenamer(int runId) {
      this.runId = runId;
    }

    public File rename(File file) {
      Joiner j = Joiner.on('_');
      return new File(String.format("%d_%s", runId, j.join(file.getPath().split(File.separator))));
    }
  }
}
