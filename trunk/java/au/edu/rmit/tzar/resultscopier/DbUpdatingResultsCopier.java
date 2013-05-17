package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;

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
  public void copyResults(Run run, File sourcePath, boolean success) {
    if (!("completed".equals(run.getState()) || "failed".equals(run.getState()))) {
      LOG.log(Level.SEVERE, "Expected run to have status: 'completed' or 'failed'. Was '{0}'. " +
          "Skipping copy for run: {1}", new Object[]{run.getState(), run});
    }

    try {
      resultsCopier.copyResults(run, sourcePath, success);
      if (success) {
        run.setState("copied");
      }
      run.setRemoteOutputPath(new File(resultsCopier.getBaseDestPath(), sourcePath.getName()));
      try {
        runDao.persistRun(run);
      } catch (TzarException e) {
        LOG.log(Level.SEVERE, "Failure updating run: " + run, e);
      }
    } catch (TzarException e) {
      LOG.log(Level.WARNING, "Error copying results for run: " + run, e);
      handleFail(run, success);
    }
  }

  private void handleFail(Run run, boolean success) {
    // we only update the status for completed runs. If the run failed, setting the status to copy_failed
    // would be misleading.
    if (!success) {
      return;
    }

    run.setState("copy_failed");
    try {
      runDao.persistRun(run);
    } catch (TzarException e) {
      LOG.log(Level.SEVERE, "Failure updating status to 'copy_failed' for run: " + run, e);
    }
  }

  @Override
  public File getBaseDestPath() {
    return resultsCopier.getBaseDestPath();
  }
}