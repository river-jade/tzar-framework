package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.db.Utils;

import java.util.List;

import static au.edu.rmit.tzar.commands.SharedFlags.*;

/**
 * Prints a list of runs from the database to stdout.
 */
public class PrintRuns implements Command {
  public static final Object[] FLAGS = new Object[]{CommandFlags.PRINT_RUNS_FLAGS, LOAD_RUNS_FLAGS,
      PRINT_TABLE_FLAGS, DB_FLAGS};

  private final RunDao runDao;
  private final List<String> states;
  private final String hostname;
  private final String runset;
  private final List<Integer> runIds;
  private final boolean truncateOutput;
  private final Utils.OutputType outputType;

  public PrintRuns(RunDao runDao, List<String> states, String hostname, String runset, List<Integer> runIds,
      boolean truncateOutput, Utils.OutputType outputType) {
    this.runDao = runDao;
    this.states = states;
    this.hostname = hostname;
    this.runset = runset;
    this.runIds = runIds;
    this.truncateOutput = truncateOutput;
    this.outputType = outputType;
  }

  @Override
  public boolean execute() throws InterruptedException, RdvException {
    runDao.printRuns(states, hostname, runset, runIds, truncateOutput, outputType);
    return true;
  }
}
