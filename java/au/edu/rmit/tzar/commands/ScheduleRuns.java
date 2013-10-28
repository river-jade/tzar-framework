package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.CommandFlags.SCHEDULE_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.CREATE_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;

/**
 * Command to create scheduled runs in the database. Reads a project specification
 * from a local json file, and schedules a run for each scenario in the spec.
 */
class ScheduleRuns implements Command {
  private static final Logger LOG = Logger.getLogger((ScheduleRuns.class.getName()));
  public static final Object[] FLAGS = new Object[]{SCHEDULE_RUNS_FLAGS, CREATE_RUNS_FLAGS, DB_FLAGS};

  private final RunDao runDao;
  private final int numRuns;
  private final RunFactory runFactory;

  public ScheduleRuns(RunDao runDao, int numRuns, RunFactory runFactory) {
    this.runDao = runDao;
    this.numRuns = numRuns;
    this.runFactory = runFactory;
  }

  @Override
  public boolean execute() throws TzarException {
    String runset = CREATE_RUNS_FLAGS.getRunset();
    List<Run> runsetRuns = runDao.getRuns(ImmutableList.<String>of(), null, runset,
        ImmutableList.<Integer>of());
    if (!runsetRuns.isEmpty() && System.console() != null) {
      String response = System.console().readLine("Runset \"%s\" already exists. " +
          "Do you wish to add runs to this runset? y/[n]: ", runset);
      if (!"y".equals(response.toLowerCase())) {
        return false;
      }
    }
    List<Run> runs = runFactory.createRuns(numRuns);
    runDao.insertRuns(runs);
    for (Run run : runs) {
      LOG.log(Level.INFO, "Scheduled run:{0} ", run);
    }
    LOG.log(Level.INFO, "Inserted {0} runs.", runs.size());
    return true;
  }
}
