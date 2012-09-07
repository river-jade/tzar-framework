package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ResultsCopier wrapper which also updates the runs database. This class is thread-safe,
 * that is, copyResults may be safely called concurrently. The run method spawns a new thread
 * that executes an endless loop which polls the copyQueue for new completed runs whose
 * results are to be copied, and so should be run in its own thread, as it does not return
 * unless the thread is interrupted.
 * TODO(michaell): unit tests
 */
public class DbUpdatingResultsCopier implements ResultsCopier, Runnable {
  private static final Logger LOG = Logger.getLogger(DbUpdatingResultsCopier.class.getName());

  private final BlockingQueue<RunAndPath> copyQueue = new LinkedBlockingQueue<RunAndPath>();
  private final ResultsCopier resultsCopier;
  private final RunDao runDao;

  public DbUpdatingResultsCopier(ResultsCopier resultsCopier, RunDao runDao) throws RdvException {
    this.resultsCopier = resultsCopier;
    this.runDao = runDao;
  }

  @Override
  public void run() {
    while (true) {
      RunAndPath runAndPath;
      try {
        runAndPath = copyQueue.take();
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, "Copy thread was interrupted.", e);
        Thread.currentThread().interrupt();
        return;
      }

      Run run = runAndPath.run;

      if (!"completed".equals(run.getState())) {
        LOG.severe("Expected run to have status: 'completed'. Was '" + run.getState() + "'. Skipping copy for run: " +
            run);
        continue;
      }

      try {
        resultsCopier.copyResults(run, runAndPath.path);
        LOG.fine("Marking run as 'copied'");
        run.setState("copied");
        run.setOutputPath(new File(resultsCopier.getBaseDestPath(), runAndPath.path.getName()));
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
  public void copyResults(Run run, File sourcePath) {
    copyQueue.add(new RunAndPath(run, sourcePath));
  }

  @Override
  public File getBaseDestPath() {
    return resultsCopier.getBaseDestPath();
  }

  private class RunAndPath {
    private final Run run;
    private final File path;

    private RunAndPath(Run run, File path) {
      this.run = run;
      this.path = path;
    }
  }
}
