package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.TzarException;

/**
 * Creates an instance of a class by class name.
 */
public class DynamicObjectFactory<T> {
  @SuppressWarnings("unchecked")
  private Class<T> cast(Class aClass) {
    return (Class<T>) aClass;
  }

  /**
   * Loads the specified class from the classpath and instantiate it. The specified
   * class must be an implementation of au.edu.rmit.tzar.api.Runner. This method
   * first attempts to find the class as a fully qualified name. If this fails, it
   * looks for the class in the au.edu.rmit.tzar.runners package.
   *
   * The class must have a no-args constructor.
   *
   * @param className name of a class in the au.edu.rmit.tzar.runners package, or
   * fully qualified classname available to this class's classloader.
   *
   * @return an instance of the provided Runner implementation
   *
   * @throws au.edu.rmit.tzar.api.TzarException if the class can not be found, or is not an instance of
   * Runner.class, or can not be instantiated.
   */
  public T getInstance(String className) throws TzarException {
    Class<T> clazz = loadClass(className);
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      throw new TzarException("Couldn't instantiate runner.", e);
    } catch (IllegalAccessException e) {
      throw new TzarException("Couldn't instantiate runner.", e);
    }
  }

  private Class<T> loadClass(String className) throws TzarException {
    Class<?> aClass;
    try {
      aClass = getClass().getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      try {
        aClass = getClass().getClassLoader().loadClass("au.edu.rmit.tzar.runners." + className);
      } catch (ClassNotFoundException e1) {
        //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
        throw new TzarException("Unable to load class: " + className);
      }
    }
    return cast(aClass);
  }
}
