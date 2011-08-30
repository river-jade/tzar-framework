package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

import java.util.logging.Logger;

/**
 * Factory to load runners by classname. Current implementation just looks up the runner
 * name from the system property 'runnerclass'.
 */
public class RunnerFactory {
  private static Logger LOG = Logger.getLogger(RunnerFactory.class.getName());

  public Runner loadRunner() throws RdvException {
    // TODO(michaell): Choose the class to load from the database instead of system property?
    String className = System.getProperty("runnerclass");
    try {
      LOG.fine("Loading runner: " + className);
      Class<?> clazz = ExecutableRun.class.getClassLoader().loadClass(className);
      return (Runner) clazz.newInstance();
    } catch (ClassNotFoundException e) {
      throw new RdvException("Couldn't find class '" + className + "' in class path.", e);
    } catch (InstantiationException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    } catch (IllegalAccessException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    }
  }
}
