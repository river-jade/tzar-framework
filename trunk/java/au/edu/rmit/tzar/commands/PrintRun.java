package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.db.RunDao;

import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.PRINT_TABLE_FLAGS;

/**
 * Prints a list of runs from the database to stdout.
 */
class PrintRun implements Command {
  public static final Object[] FLAGS = new Object[]{CommandFlags.PRINT_RUN_FLAGS, PRINT_TABLE_FLAGS, DB_FLAGS};

  private final RunDao runDao;
  private final Integer runId;
  private final boolean truncateOutput;

  public PrintRun(RunDao runDao, int runId, boolean truncateOutput) {
    this.runDao = runDao;
    this.runId = runId;
    this.truncateOutput = truncateOutput;
  }

  @Override
  public boolean execute() throws InterruptedException, RdvException {
    runDao.printRun(runId, truncateOutput);
    return true;
  }
}
