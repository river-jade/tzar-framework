package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runner that sleeps for 5 seconds then succeeds.
 */
public class SleepRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {
    try {
      final Thread outerThread = Thread.currentThread();
      stopRun.registerStopTask(new Runnable() {
        @Override
        public void run() {
          outerThread.interrupt();
        }
      });
      Thread.sleep(5000);
      return true;
    } catch (InterruptedException e) {
      logger.log(Level.FINE, "Sleep interrupted");
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
