package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.DynamicObjectFactory;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.TzarException;

/**
 * Factory to create runners by classname. This class currently just wraps
 * the call to newInstance to hide the exception handling.
 */
public class RunnerFactory extends DynamicObjectFactory<Runner> {
  public Runner getRunner(String runnerClass) throws TzarException {
    return getInstance(runnerClass);
  }
}
