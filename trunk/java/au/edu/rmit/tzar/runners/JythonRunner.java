package au.edu.rmit.tzar.runners;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Runner;
import com.google.common.io.Files;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A runner which calls out to a Runner implemented in Jython code.
 * The runner must be implemented in a file "modelrunner.py", and the class
 * name must be ModelRunner. Output from the runner will be logged to a file
 * in the output directory.
 */
public class JythonRunner implements Runner {
  private static Logger LOG = Logger.getLogger(JythonRunner.class.getName());

  @Override
  public boolean runModel(File model, File outputPath, String runId, String runnerFlags, Parameters parameters,
      Logger logger) throws TzarException {
    Runner runner;
    try {
      runner = getJythonModelRunner(model);
    } catch (IOException e) {
      throw new TzarException("Error loading the Jython model.", e);
    }

    try {
      return runner.runModel(model, outputPath, runId, runnerFlags, parameters, logger);
    } catch (PyException e) {
      throw new TzarException("Error occurred running the jython code.", e);
    }
  }

  private Runner getJythonModelRunner(File model) throws IOException, TzarException {
    PythonInterpreter interpreter = new PythonInterpreter();

    PySystemState sys = Py.getSystemState();
    sys.path.append(new PyString(model.getAbsolutePath()));

    interpreter.setOut(System.out);
    interpreter.setErr(System.err);

    File tempDirectory = Files.createTempDir();
    sys.path.append(new PyString(tempDirectory.getAbsolutePath()));
    File modelRunner = RunnerUtils.extractResourceToFile(tempDirectory, "jython/", "modelrunner.py");
    RunnerUtils.extractResourceToFile(tempDirectory, "jython/", "basemodel.py");
    RunnerUtils.extractResourceToFile(tempDirectory, "jython/", "rrunner.py");
    interpreter.execfile(modelRunner.toString());
    interpreter.exec("modelrunner=ModelRunner()");
    try {
      Class runnerClass = Class.forName("au.edu.rmit.tzar.api.Runner");
      return (Runner) interpreter.get("modelrunner").__tojava__(runnerClass);
    } catch (ClassNotFoundException e) {
      throw new IOException("Unable to load jython class.", e);
    }
  }
}
