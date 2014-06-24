package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.MapReduce;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.CommandFlags.EXEC_LOCAL_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.CREATE_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.RUNNER_FLAGS;

/**
 * Execute a set of runs locally (ie without contacting the runs database).
 */
public class ExecLocalRuns implements Command {
  private static Logger LOG = Logger.getLogger(ExecLocalRuns.class.getName());
  public static final Object[] FLAGS = new Object[]{EXEC_LOCAL_RUNS_FLAGS, CREATE_RUNS_FLAGS, RUNNER_FLAGS};

  private final RunFactory runFactory;
  private final Optional<au.edu.rmit.tzar.api.MapReduce> mapReduce;
  private final int numRuns;
  private final File tzarModelPath;
  private final RunnerFactory runnerFactory;
  private final File tzarOutputPath;
  private final StopRun stopRun;

  public ExecLocalRuns(int numRuns, RunFactory runFactory, File tzarOutputPath, File tzarModelPath,
      RunnerFactory runnerFactory, Optional<MapReduce> mapReduce, StopRun stopRun) throws TzarException, IOException {
    this.numRuns = numRuns;
    this.tzarOutputPath = tzarOutputPath;
    this.tzarModelPath = tzarModelPath;
    this.runnerFactory = runnerFactory;
    this.runFactory = runFactory;
    this.mapReduce = mapReduce;
    this.stopRun = stopRun;
  }

  @Override
  public boolean execute() throws InterruptedException, TzarException {
    List<Run> runs = runFactory.createRuns(numRuns);
    List<Integer> failedIds = Lists.newArrayList();

    // if another thread stops the run (by calling stopRun.stop(), finish after the current
    // run
    final AtomicBoolean stopped = new AtomicBoolean(false);
    stopRun.registerStopTask(new Runnable() {
      @Override
      public void run() {
        stopped.set(true);
      }
    });

    int completeCount = 0;
    for (Run run : runs) {
      if (stopped.get()) {
        break;
      }
      ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tzarOutputPath, tzarModelPath,
          runnerFactory);
      if (!executableRun.execute(stopRun)) {
        failedIds.add(run.getRunId());
      }
      completeCount++;
    }

    File runsetOutputPath = Utils.createRunsetOutputPath(tzarOutputPath, runFactory.getProjectName(),
        runFactory.getRunset());

    if (mapReduce.isPresent()) {
      if (failedIds.isEmpty()) {
        mapReduce.get().execute(runs, runsetOutputPath);
      } else {
        LOG.warning("Did not execute map-reduce as one or more of the runs failed.");
      }
    }

    Level level;
    int failed = failedIds.size();
    boolean allSuccess = failed == 0;
    if (allSuccess) {
      level = Level.INFO;
    } else {
      level = Level.WARNING;
    }
    LOG.log(level, "Executed {0} runs: {1} succeeded. {2} failed", new Object[]{completeCount, completeCount - failed,
        failed});
    if (!allSuccess) {
      LOG.warning("Failed IDs were: " + failedIds);
    }
    return allSuccess;
  }
}
