package au.edu.rmit.tzar;

import java.io.File;

/**
 * Constants for the RDV package.
 */
public class Constants {
  public static final String DB_ENVIRONMENT_VARIABLE_NAME = "TZAR_DB";

  public static final File DEFAULT_TZAR_BASE_DIR = new File(System.getProperty("user.home"), "tzar");

  // Default directory name to use for the model code
  public static final String DEFAULT_MODEL_CODE_DIR = "modelcode";

  // Name of output directory for runs executed using execlocalruns
  public static final String LOCAL_OUTPUT_DATA_DIR = "outputdata";

  // name of output directory for runs executed using pollandrun.
  public static final String POLL_AND_RUN_OUTPUT_DIR = "pollandrun_outputdata";

  // Default cluster name to use when none is specified
  public static final String DEFAULT_CLUSTER_NAME = "default";

  // Default runset name to use when none is specified
  public static final String DEFAULT_RUNSET = "default_runset";

  // Port on which to run the embedded webserver
  public static final int WEBSERVER_PORT = 8080;

  // Directory suffixes for run output directory names
  public static final String INPROGRESS_SUFFIX = ".inprogress";
  public static final String FAILED_SUFFIX = ".failed";
}
