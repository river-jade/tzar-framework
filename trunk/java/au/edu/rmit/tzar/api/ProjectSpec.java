package au.edu.rmit.tzar.api;

import java.util.List;
import java.util.Map;

/**
 * Specification for a single Tzar project. Along with the model code, and the libraries pointed to
 * by the libraries map, this contains enough information to create a set of Tzar runs for this project.
 */
public interface ProjectSpec {
  Parameters getBaseParams();

  Map<String, ? extends CodeSource> getLibraries();

  String getProjectName();

  Repetitions getRepetitions();

  String getRunnerClass();

  String getRunnerFlags();

  /**
   * Gets the list of scenarios for this project specification. Scenarios are required, and this list should
   * be non-empty.
   *
   * @return
   */
  List<Scenario> getScenarios();
}
