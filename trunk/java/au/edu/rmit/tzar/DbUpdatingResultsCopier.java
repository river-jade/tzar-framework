package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ResultsCopier wrapper which also updates the runs database. 
 * TODO(michaell): unit tests
 */
public class DbUpdatingResultsCopier implements ResultsCopier {
  private static final Logger LOG = Logger.getLogger(DbUpdatingResultsCopier.class.getName());

  private final ResultsCopier resultsCopier;
  private final RunDao runDao;

  public DbUpdatingResultsCopier(ResultsCopier resultsCopier, RunDao runDao) {
    this.resultsCopier = resultsCopier;
    this.runDao = runDao;
  }

  @Override
  public void copyResults(Run run, File sourcePath) {
    if (!"completed".equals(run.getState())) {
      LOG.severe("Expected run to have status: 'completed'. Was '" + run.getState() + "'. Skipping copy for run: " +
          run);
    }

    try {
      resultsCopier.copyResults(run, sourcePath);
      run.setState("copied");
      run.setOutputPath(new File(resultsCopier.getBaseDestPath(), sourcePath.getName()));
      try {
        runDao.persistRun(run);
      } catch (RdvException e) {
        LOG.log(Level.SEVERE, "Failure updating run: " + run, e);
      }
    } catch (RdvException e) {
      LOG.log(Level.WARNING, "Error copying results for run: " + run, e);
      handleFail(run);
    }
  }

  private void handleFail(Run run) {
    run.setState("copy_failed");
    try {
      runDao.persistRun(run);
    } catch (RdvException e) {
      LOG.log(Level.SEVERE, "Failure updating status to 'copy_failed' for run: " + run, e);
    }
  }

  @Override
  public File getBaseDestPath() {
    return resultsCopier.getBaseDestPath();
  }
}
