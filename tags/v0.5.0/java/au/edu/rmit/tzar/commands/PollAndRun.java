package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.db.RunDao;
import au.edu.rmit.tzar.resultscopier.ResultsCopier;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.base.Optional;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.CommandFlags.POLL_AND_RUN_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.DB_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.RUNNER_FLAGS;

/**
 * PollAndRun polls the database for jobs awaiting execution, then downloads the appropriate version of the framework
 * for the job (if not already cached locally), and executes that job with the appropriate parameters.
 */
class PollAndRun implements Command {
  private static final Logger LOG = Logger.getLogger(PollAndRun.class.getName());
  public static final Object[] FLAGS = new Object[]{CommandFlags.POLL_AND_RUN_FLAGS, DB_FLAGS, RUNNER_FLAGS};

  private final RunDao runDao;
  private final int pollRateMs;
  private final Optional<String> runset;
  private final ResultsCopier resultsCopier;
  private final String clusterName;

  // local path for output for all tzar runs
  private final File tzarOutputPath;
  private final File baseModelPath;
  private final RunnerFactory runnerFactory;
  private final List<String> repositoryUriPrefixes;

  /**
   * Constructor.
   * @param runDao for accessing the database
   * @param pollRateMs milliseconds to wait between polls
   * @param resultsCopier to copy the results from this node to permanent storage
   * @param runset name of the runset to poll, or null to poll all runsets
   * @param clusterName name of the cluster which this node is running on
   * @param tzarOutputPath base local path for output of the runs
   * @param runnerFactory to create runners
   * @param repositoryUriPrefixes list of allowed repository uri prefixes
   */
  public PollAndRun(RunDao runDao, int pollRateMs, ResultsCopier resultsCopier, Optional<String> runset,
      String clusterName, File tzarOutputPath, File baseModelPath, RunnerFactory runnerFactory,
      List<String> repositoryUriPrefixes) {
    this.tzarOutputPath = tzarOutputPath;
    this.baseModelPath = baseModelPath;
    this.runnerFactory = runnerFactory;
    this.runDao = runDao;
    this.pollRateMs = pollRateMs;
    this.runset = runset;
    this.clusterName = clusterName;
    this.resultsCopier = resultsCopier;
    this.repositoryUriPrefixes = repositoryUriPrefixes;
  }

  /**
   * Constructor. Reads parameters from POLL_AND_RUN_FLAGS. This object must be initialised before
   * calling this constructor.
   *
   * @param runDao for accessing the database
   * @param resultsCopier to copy the results from this node to permanent storage
   * @param tzarOutputPath base local path for output of the runs
   * @param baseModelPath base local path for the model code
   * @param runnerFactory to create runners
   */
  public PollAndRun(RunDao runDao, ResultsCopier resultsCopier,
      File tzarOutputPath, File baseModelPath, RunnerFactory runnerFactory) throws TzarException {
    this.tzarOutputPath = tzarOutputPath;
    this.runnerFactory = runnerFactory;
    this.runDao = runDao;
    this.baseModelPath = baseModelPath;
    this.pollRateMs = POLL_AND_RUN_FLAGS.getPollRateMs();
    this.runset = POLL_AND_RUN_FLAGS.getRunset();
    this.clusterName = POLL_AND_RUN_FLAGS.getClusterName();
    this.resultsCopier = resultsCopier;
    repositoryUriPrefixes = POLL_AND_RUN_FLAGS.getRepositoryUriPrefixes();
  }

  /**
   * Polls the database for new runs, and loops until exitAfterCurrent run is called (and current run finishes).
   * Sleeps 10 seconds between each poll if there are no runs to execute.
   *
   * @throws InterruptedException if the thread is interrupted
   */
  @Override
  public boolean execute() throws InterruptedException {
    final Spinner spinner = new Spinner().start();
    final CountDownLatch stop = new CountDownLatch(1);
    final Timer timer = new Timer();
    final TimerTask task = new PollTask(stop, spinner, timer);
    timer.schedule(task, 0 /* start polling straightaway */);

    stop.await(); // wait until the poll task signals that it's done
    return true;
  }

  private void executeRun(final Run run) throws TzarException, InterruptedException {
    ExecutableRun executableRun = ExecutableRun.createExecutableRun(run, tzarOutputPath, baseModelPath, runnerFactory);

    run.setStartTime(new Date());
    run.setEndTime(null);
    run.setState(Run.State.IN_PROGRESS);
    run.setHostname(Utils.getHostname());
    run.setHostIp(Utils.getHostIp());
    if (!runDao.markRunInProgress(run)) {
      // another node must have grabbed the job.
      return;
    }

    new DbExecutableRun(executableRun, resultsCopier, runDao, repositoryUriPrefixes).run();
  }

  /**
   * Poll the database and execute runs until there are no more runs in the scheduled state.
   * @throws TzarException
   * @throws InterruptedException
   */
  private void pollUntilDone() throws TzarException, InterruptedException {
    while (true) {
      LOG.finer("Polling for next run.");
      Optional<Run> run = runDao.getNextRun(runset, clusterName);
      if (!run.isPresent()) {
        return;
      }
      executeRun(run.get());
    }
  }

  /**
   * A Runnable (ie implements Runnable) wrapper around ExecutableRun which
   * updates the database upon completion.
   * TODO(river): this class exists as a legacy of the incomplete parallel run execution
   * code. Either finish that work or remove this class.
   */
  private static class DbExecutableRun implements Runnable {
    private final ExecutableRun executableRun;
    private final ResultsCopier resultsCopier;
    private final Run run;
    private final RunDao runDao;
    private final List<String> repositoryUriPrefixes;

    public DbExecutableRun(ExecutableRun executableRun, ResultsCopier resultsCopier, RunDao runDao,
        List<String> repositoryUriPrefixes) {
      this.executableRun = executableRun;
      this.resultsCopier = resultsCopier;
      this.runDao = runDao;
      this.repositoryUriPrefixes = repositoryUriPrefixes;
      this.run = executableRun.getRun();
    }

    @Override
    public void run() {
      boolean success = false;
      try {
        success = checkUriPrefixes(run) && executableRun.execute();
      } catch (TzarException e) { // note: we eat these exceptions because we don't want to kill the thread.
        LOG.log(Level.SEVERE, "Error occurred executing run: " + run.getRunId(), e);
        System.out.println("\n");
      } catch (RuntimeException e) {
        LOG.log(Level.SEVERE, "Runtime exception occurred executing run: " + run.getRunId(), e);
      } finally {
        run.setEndTime(new Date());
        if (success) {
          LOG.info("Run " + run.getRunId() + " succeeded.");
          run.setState(Run.State.COMPLETED);
        } else {
          LOG.warning("Run " + run.getRunId() + " failed.");
          run.setState(Run.State.FAILED);
        }

        try {
          Utils.Retryable.retryWithBackoff(5/* retry attempts */ ,
              5000/* initial backoff */,
              new Utils.Retryable() {
            public void exec() throws TzarException {
              runDao.persistRun(run);
            }
          });
        } catch (TzarException e) {
          LOG.log(Level.SEVERE, "Error occurred persisting run status change for Run:" + run.getRunId() +
              " to database. Run status will be invalid.", e);
          System.out.println("\n");
        }
      }

      try {
        resultsCopier.copyResults(run, executableRun.getOutputPath(), success);
      } catch (TzarException e) {
        LOG.log(Level.WARNING, "Failed to copy the results for run: " + run.getRunId(), e);
      }
    }

    /**
     * Make sure that the run source URL begins with one of the allowed prefixes. Otherwise, fail the run.
     * @param run the run to check.
     * @return true if the run is ok
     */
    private boolean checkUriPrefixes(Run run) {
      String sourceUri = run.getCodeSource().getSourceUri().toString();
      for (String prefix : repositoryUriPrefixes) {
        if (sourceUri.startsWith(prefix)) {
          return true;
        }
      }
      LOG.warning("The sourceUrl for this run (" + sourceUri + ") did not begin with any of the allowed prefixes (" +
          repositoryUriPrefixes + "). Failing run as a security measure.");
      return false;
    }
  }

  private class PollTask extends TimerTask {
    private final CountDownLatch stop;
    private final Spinner spinner;
    private final Timer timer;
    private int pollInterval;

    public PollTask(CountDownLatch stop, Spinner spinner, Timer timer) {
      this(stop, spinner, timer, pollRateMs);
    }

    private PollTask(CountDownLatch stop, Spinner spinner, Timer timer, int pollInterval) {
      this.stop = stop;
      this.spinner = spinner;
      this.timer = timer;
      this.pollInterval = pollInterval;
    }

    @Override
    public void run() {
      if (checkForStopFile()) {
        stop.countDown();
        return;
      }
      // stop the spinner while we poll.
      spinner.pause();
      // keep polling until there are no more runs
      int nextPollInterval;
      try {
        pollUntilDone();
        nextPollInterval = pollRateMs; // success! reset poll interval to standard
      } catch (Exception e) {
        System.out.println("\n"); // so that the exception is on a new line
        LOG.log(Level.SEVERE, "Error occurred executing run.", e);
        // exponentially back off the poll interval up to a maximum.
        nextPollInterval = Math.min(pollInterval * 2, Constants.MAX_POLL_INTERVAL_MS);
        LOG.log(Level.INFO, "Polling again in {0} seconds.", nextPollInterval / 1000);
      } finally {
        spinner.resume();
      }
      TimerTask task = new PollTask(stop, spinner, timer, nextPollInterval);
      timer.schedule(task, nextPollInterval);
    }

    private boolean checkForStopFile() {
      File stopFile = new File(System.getProperty("user.dir"), Constants.STOP_FILE_NAME);
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
  }

  private static class Spinner extends TimerTask {
    private static final String BACKSPACE = "\b";
    private static final char[] SYMBOL = new char[]{'-', '\\', '|', '/'};
    private int spinCounter = 0;
    private boolean first = true;
    private volatile boolean renderSpinner = true;
    private Timer timer = new Timer();

    public Spinner start() {
      renderSpinner = true;
      int interval = Constants.SPINNER_ROTATION_INTERVAL_MS;
      timer.scheduleAtFixedRate(this, interval, interval);
      return this;
    }

    public Spinner pause() {
      renderSpinner = false;
      return this;
    }

    public Spinner resume() {
      renderSpinner = true;
      return this;
    }

    @Override
    public void run() {
      if (renderSpinner) {
        System.out.print((first ? "" : BACKSPACE) + SYMBOL[spinCounter++ % 4]);
        System.out.flush();
      }
    }
  }
}
