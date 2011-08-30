package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;

import java.io.IOException;
import java.util.List;
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

  public ScheduleRuns(RunDao runDao, int numRuns, RunFactory runFactory) throws RdvException, IOException {
    this.runDao = runDao;
    this.numRuns = numRuns;
    this.runFactory = runFactory;
  }

  @Override
  public boolean execute() throws RdvException {
    List<Run> runs = runFactory.createRuns(numRuns);
    runDao.insertRuns(runs);
    for (Run run : runs) {
      LOG.info("Scheduled run: " + run);
    }
    LOG.info(String.format("Inserted %d runs.", runs.size()));
    return true;
  }
}
