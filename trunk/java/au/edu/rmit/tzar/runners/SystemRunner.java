package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base class for Runners which write parameters out to a file and then call a
 * system command (eg Python, or R) and run a script.
 */
public abstract class SystemRunner implements Runner {
  protected boolean executeCommand(File model, Logger logger, Map<String, String> environment, StopRun stopRun,
      String... command) throws TzarException {
    try {
      logger.fine(Joiner.on(" ").join(command));
      ProcessBuilder processBuilder = new ProcessBuilder(command);

      processBuilder.environment().putAll(environment);
      final Process process = processBuilder
          .redirectErrorStream(true)
          .directory(model)
          .start();

      // Send stdout and stderr to the logger.
      ExecutorService executor = Executors.newCachedThreadPool();
      Utils.copyStreamToLog(process.getInputStream(), logger, Level.FINE, stopRun, executor);
      Utils.copyStreamToLog(process.getErrorStream(), logger, Level.WARNING, stopRun, executor);
      stopRun.registerStopTask(new Runnable() {
        @Override
        public void run() {
          // kills the running external process
          process.destroy();
        }
      });

      int returnValue = process.waitFor();
      executor.shutdown();
      return returnValue == 0;
    } catch (IOException e) {
      throw new TzarException(e);
    } catch (InterruptedException e) {
      throw new TzarException(e);
    }
  }
}
