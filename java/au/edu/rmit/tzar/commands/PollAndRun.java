package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.DbUpdatingResultsCopier;
import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.repository.CodeRepository;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.RUNNER_FLAGS;

/**
 * PollAndRun polls the database for jobs awaiting execution, then downloads the appropriate version of the framework
 * for the job (if not already cached locally), and executes that job with the appropriate parameters.
 */
class PollAndRun implements Command {
  private static final Logger LOG = Logger.getLogger(PollAndRun.class.getName());
  private static final String STOP_FILE_NAME = "stop.now";
  public static final Object[] FLAGS = new Object[]{CommandFlags.POLL_AND_RUN_FLAGS, DB_FLAGS, RUNNER_FLAGS};

  private final RunDao runDao;
  private final AtomicBoolean exitWhenDone = new AtomicBoolean(false);
  private final CountDownLatch finished = new CountDownLatch(1);
  private final int sleepTimeMillis;
  private final String runset;
  private final DbUpdatingResultsCopier resultsCopier;
  private final ExecutorService executorService;
  private final Semaphore runningTasks;
  private final String clusterName;
  private final File baseOutputPath;
  private final CodeRepository codeRepository;
  private final RunnerFactory runnerFactory;

  public PollAndRun(DaoFactory daoFactory, int sleepTimeMillis, ResultsCopier resultsCopier, String runset,
                    String clusterName, int concurrentTaskCount, File baseOutputPath, CodeRepository codeRepository,
                    RunnerFactory runnerFactory)
      throws RdvException, IOException {
    this.baseOutputPath = baseOutputPath;
    this.codeRepository = codeRepository;
    this.runnerFactory = runnerFactory;
    this.runDao = daoFactory.createRunDao();
    this.sleepTimeMillis = sleepTimeMillis;
    this.runset = runset;
    this.clusterName = clusterName;
    this.resultsCopier = new DbUpdatingResultsCopier(resultsCopier, daoFactory.createRunDao());
    executorService = Executors.newFixedThreadPool(concurrentTaskCount);
    runningTasks = new Semaphore(concurrentTaskCount);
  }

  /**
   * Polls the database for new runs, and loops until exitAfterCurrent run is called (and current run finishes).
   * Sleeps 10 seconds between each poll if there are no runs to execute.
   *
   * @throws InterruptedException if the thread is interrupted
   */
  public boolean execute() throws InterruptedException {
    // start the file copier thread
    new Thread(resultsCopier).start();
    int spinnerDelay = 1000; // rotate spinner on stdout every 1 sec.
    try {
      int spinCounter = 0;
      int pollCounter = 0;
      char[] symbol = new char[]{'-', '\\', '|', '/'};
      String backspace = "";
      while (!exitWhenDone.get()) {
        if (checkForStopFile()) {
          return true;
        }
        LOG.finer("Polling for next run.");
        Run run = null;
        try {
          while (true) {
            runningTasks.acquire(); // this line will block if there are already max tasks running
            run = runDao.getNextRun(runset, clusterName);
            if (run == null) {
              runningTasks.release();
              break;
            }
            executeRun(run);
          }
          if (spinCounter * spinnerDelay >= pollCounter * sleepTimeMillis) {
            pollCounter++;
          }
        } catch (RdvException e) {
          LOG.log(Level.SEVERE, "Error occurred executing run.", e);
          System.out.println("\n");
          runningTasks.release();
        }

        if (run == null) { // no run found. wait for 10 secs.
          Thread.sleep(spinnerDelay);
          System.out.print(backspace + symbol[spinCounter++ % 4]);
          System.out.flush();
          backspace = "\b";
        } else {
          backspace = "";
        }
      }
    } finally {
      finished.countDown();
    }
    return true;
  }

  private static boolean checkForStopFile() {
    File stopFile = new File(System.getProperty("user.dir"), STOP_FILE_NAME);
    if (stopFile.exists()) {
      if (!stopFile.delete()) {
        LOG.warning("Could not delete stop file: " + stopFile);
      }
      LOG.info("Found stop file. Exiting.");
      return true;
    } else {
      return false;
    }
  }

  private void executeRun(final Run run) throws RdvException, InterruptedException {
    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, baseOutputPath, codeRepository,
        runnerFactory.loadRunner());

    run.setStartTime(new Date());
    run.setEndTime(null);
    run.setState("in_progress");
    run.setHostname(Utils.getHostname());
    runDao.persistRun(run);

    executorService.submit(new DbExecutableRun(executableRun, resultsCopier, runDao, new Callback() {
      @Override
      public void complete() {
        runningTasks.release(); // release the semaphore now that the task is done
      }
    }));
  }

  private static interface Callback {
    void complete();
  }

  /**
   * A Runnable (ie implements Runnable) wrapper around ExecutableRun which
   * updates the database upon completion.
   */
  private static class DbExecutableRun implements Runnable {
    private final ExecutableRun executableRun;
    private final DbUpdatingResultsCopier resultsCopier;
    private final Run run;
    private final RunDao runDao;
    private final Callback callback;

    public DbExecutableRun(ExecutableRun executableRun, DbUpdatingResultsCopier resultsCopier, RunDao runDao,
        Callback callback) {
      this.executableRun = executableRun;
      this.resultsCopier = resultsCopier;
      this.runDao = runDao;
      this.callback = callback;
      this.run = executableRun.getRun();
    }

    @Override
    public void run() {
      boolean success = false;
      try {
        success = executableRun.execute();
      } catch (RdvException e) {
        LOG.log(Level.SEVERE, "Error occurred executing run: " + run.getRunId(), e);
        System.out.println("\n");
      } finally {
        run.setEndTime(new Date());
        if (success) {
          LOG.info("Run " + run.getRunId() + " succeeded.");
          run.setState("completed");
        } else {
          LOG.warning("Run " + run.getRunId() + " failed.");
          run.setState("failed");
        }

        try {
          runDao.persistRun(run);
        } catch (RdvException e) {
          LOG.log(Level.SEVERE, "Error occurred persisting run status change for Run:" + run.getRunId() +
              " to database. Run status will be invalid.", e);
          System.out.println("\n");
        }
      }
      if (success) {
        resultsCopier.copyResults(run, executableRun.getOutputPath());
      }
      callback.complete();
    }
  }
}
