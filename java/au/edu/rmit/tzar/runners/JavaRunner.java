package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Runner;
import au.edu.rmit.tzar.api.StopRun;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.Parameter;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.proxy.CglibProxyProvider;
import org.xeustechnologies.jcl.proxy.ProxyProviderFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Java runner implementation. This is used to dynamically load a Java
 * module from a jar file and execute its own Runner.
 * The name of the jar file is passed as --jarpath in the flagString,
 * and the fully qualified name of the java class to execute is passed
 * as --classname, unless the default of RunnerImpl (in the root package) is used.
 */
public class JavaRunner implements Runner {
  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger, StopRun stopRun) throws TzarException {

    Flags flags = RunnerUtils.parseFlags(runnerFlags.split(" "), new Flags());
    File jarPath = new File(model, flags.jarPath);

    try {
      if (!jarPath.getCanonicalPath().startsWith(model.getCanonicalPath())) {
        throw new TzarException("Possible path traversal attack. Aborting run.");
      }
    } catch (IOException e) {
      throw new TzarException("Couldn't canonicalise pathname. Aborting run.", e);
    }

    Runner runner = getRunner(jarPath, flags.className);
    return runner.runModel(model, outputPath, runId, runnerFlags, parameters, logger, stopRun);
  }

  private Runner getRunner(File jarPath, String runnerClassName) {
    JarClassLoader jcl = new JarClassLoader();

    String absolutePath = jarPath.getAbsolutePath();

    jcl.add(absolutePath); // Load jar file

    // Set proxy provider default to cglib (from version 2.2.1)
    ProxyProviderFactory.setDefaultProxyProvider(new CglibProxyProvider());

    //Create a factory of castable objects/proxies
    JclObjectFactory factory = JclObjectFactory.getInstance(true);

    //Create and cast object of loaded class
    return (Runner) factory.create(jcl, runnerClassName);
  }

  @com.beust.jcommander.Parameters(separators = "= ")
  private static class Flags {
    /**
     * Path to the module jar file.
     */
    @Parameter(names = "--jarpath", description = "Relative path to the project jar file", required=true)
    private String jarPath;

    /**
     * Java class name to execute. Default: RunnerImpl. Note that the default is tzar.RunnerImpl,
     * rather than RunnerImpl because JCL currently has a bug that makes it explode for classes
     * that are in the root package. https://github.com/kamranzafar/JCL/issues/7
     */
    @Parameter(names = "--classname", description = "Fully qualified java class name to execute. " +
        "Default: tzar.RunnerImpl")
    private String className = "tzar.RunnerImpl";
  }
}
