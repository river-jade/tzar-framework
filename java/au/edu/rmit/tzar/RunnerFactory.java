package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

/**
 * Factory to create runners by classname. This class currently just wraps
 * the call to newInstance to hide the exception handling.
 */
public class RunnerFactory {
  private final Class<Runner> runnerClass;

  public RunnerFactory(Class<Runner> runnerClass) {
    this.runnerClass = runnerClass;
  }

  public Runner loadRunner() throws RdvException {
    // TODO(michaell): Choose the class to load from the database instead?
    try {
      return runnerClass.newInstance();
    } catch (InstantiationException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    } catch (IllegalAccessException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    }
  }
}
