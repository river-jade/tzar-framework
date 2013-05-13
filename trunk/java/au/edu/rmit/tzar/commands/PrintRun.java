package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.db.ParametersDao;
import au.edu.rmit.tzar.db.Utils;

import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.PRINT_TABLE_FLAGS;

/**
 * Prints the parameters for a particular run from the database to stdout.
 */
class PrintRun implements Command {
  public static final Object[] FLAGS = new Object[]{CommandFlags.PRINT_RUN_FLAGS, PRINT_TABLE_FLAGS, DB_FLAGS};

  private final Integer runId;
  private final boolean truncateOutput;
  private final Utils.OutputType outputType;
  private final ParametersDao parametersDao;

  public PrintRun(ParametersDao parametersDao, int runId, boolean truncateOutput, Utils.OutputType outputType) {
    this.parametersDao = parametersDao;
    this.runId = runId;
    this.truncateOutput = truncateOutput;
    this.outputType = outputType;
  }

  @Override
  public boolean execute() throws InterruptedException, TzarException {
    synchronized (parametersDao) {
      parametersDao.printParameters(runId, truncateOutput, outputType);
    }
    return true;
  }
}
