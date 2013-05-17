package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Runner;

/**
 * Factory to create runners by classname. This class currently just wraps
 * the call to newInstance to hide the exception handling.
 */
public class RunnerFactory {
  /**
   * Loads the specified class from the classpath and instantiate it. The specified
   * class must be an implementation of au.edu.rmit.tzar.api.Runner. This method
   * first attempts to find the class as a fully qualified name. If this fails, it
   * looks for the class in the au.edu.rmit.tzar.runners package.
   *
   * The class must have a no-args constructor.
   *
   * @param runnerClass name of a class in the au.edu.rmit.tzar.runners package, or
   * fully qualified classname available to this class's classloader.
   *
   * @return an instance of the provided Runner implementation
   *
   * @throws RdvException if the class can not be found, or is not an instance of
   * Runner.class, or can not be instantiated.
   */
  public Runner getRunner(String runnerClass) throws RdvException {
    Class<Runner> clazz = loadClass(runnerClass);
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    } catch (IllegalAccessException e) {
      throw new RdvException("Couldn't instantiate runner.", e);
    }
  }

  private Class<Runner> loadClass(String className) throws RdvException {
    Class<?> aClass;
    try {
      aClass = getClass().getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      try {
        aClass = getClass().getClassLoader().loadClass("au.edu.rmit.tzar.runners." + className);
      } catch (ClassNotFoundException e1) {
        //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
        throw new RdvException("Unable to load Runner class: " + className);
      }
    }
    if (!Runner.class.isAssignableFrom(aClass)) {
      throw new RdvException("Specified class: " + className + " was not an instance of " + Runner.class);
    }
    return cast(aClass);
  }

  @SuppressWarnings("unchecked")
  private static Class<Runner> cast(Class aClass) {
    return (Class<Runner>) aClass;
  }
}
