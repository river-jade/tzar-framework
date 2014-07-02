package au.edu.rmit.tzar.api;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Construct to allow runs to be stopped mid run from another thread. The thread doing the stopping
 * creates this object and passes it into a runner, which populates the stopTasks before
 * starting the run.
 */
public class StopRun {
  private final Queue<Runnable> stopTasks = new ConcurrentLinkedQueue<Runnable>();

  /**
   * Register a runnable to be executed when stop() is called. This runnable will be run
   * in the same thread that calls stop(), and so must be coded accordingly.
   * The runnables will be executed in the order in which they are registered (ie FIFO),
   * and this may affect whether or not things work / deadlock etc.
   * @param runnable
   */
  public void registerStopTask(Runnable runnable) {
    stopTasks.add(runnable);
  }

  /**
   * Stops the run to which this StopRun object is passed, by executing each of the passed in stop tasks
   * in sequence (in the current thread).
   */
  public void stop() {
    for (Runnable runnable : stopTasks) {
      runnable.run();
    }
  }

  /**
   * Removes all stop tasks from the queue so that this stopRun can be reused.
   */
  public void reset() {
    stopTasks.clear();
  }
}
