package au.edu.rmit.tzar;

import java.io.File;

/**
 * Constants for the RDV package.
 */
public class Constants {
  public static final String DB_ENVIRONMENT_VARIABLE_NAME = "TZAR_DB";

  public static final File DEFAULT_TZAR_BASE_DIR = new File(System.getProperty("user.home"), "tzar");

  public static final String DEFAULT_MODEL_CODE_DIR = "modelcode";
  public static final String DEFAULT_OUTPUT_DATA_DIR = "outputdata";
  public static final String DEFAULT_CLUSTER_NAME = "default";
  public static final String DEFAULT_RUNSET = "default_runset";
}
