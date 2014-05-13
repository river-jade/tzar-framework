package au.edu.rmit.tzar.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a set of parameters for a run.
 */
public class Parameters {
  private static final Logger LOG = Logger.getLogger(Parameters.class.getName());
  public static final Parameters EMPTY_PARAMETERS = new Parameters(ImmutableMap.<String, Object>of());

  private final ImmutableMap<String, Object> parameters;

  /**
   * Factory method to create a new Parameters object. Verifies that there are no duplicate keys.
   *
   * @param parameters
   * @return a new Parameters object
   * @throws TzarException if there are duplicate keys
   */
  public static Parameters createParameters(Map<String, ?> parameters) {
    return new Parameters(ImmutableMap.copyOf(parameters));
  }

  /**
   * Private constructor. Key names across the maps must be unique.
   */
  private Parameters(ImmutableMap<String, Object> parameters) {
    this.parameters = parameters;
  }

  /**
   * Returns a map of keys to parameters (variables, input_files and output_files),
   * where the input and output filenames are qualified
   * by the provided baseInputPath and baseOutputPaths respectively.
   *
   * This method is deprecated, since we no longer have qualified params. This will simply return
   * the parameters map. Use {@link #asMap} instead.
   *
   * @param baseInputPath  the absolute base path for input files
   * @param baseOutputPath the absolute base path for output files
   * @return a Map of qualified parameters
   */
  @Deprecated
  public Map<String, Object> getQualifiedParams(File baseInputPath, File baseOutputPath) {
    return parameters;
  }

  /**
   * Map of keys to values of type String, Integer, Boolean or BigDecimal.
   */
  public ImmutableMap<String, Object> asMap() {
    return parameters;
  }

  /**
   * Gets the total number of parameters defined.
   *
   * @return total number of parameters
   */
  public int getSize() {
    return parameters.size();
  }

  /**
   * Merges two Parameters objects. This Object is considered to be the "base"
   * and the other the "override". This method returns a new Parameters object constructed
   * from the unions of each of the maps contained in each Parameters object (that is, a new
   * Parameters object containing the union of the 'variables' map from each object,
   * the union of the 'inputFiles' map from each object, and the union of the 'outputFiles' map
   * from each object. Where there is a key collision between two maps, the value will be chosen
   * from the overrideParameters object's map.
   *
   * @param overrideParameters
   * @return a new Parameters object containing a merged version of the two provided Parameters
   *         objects
   */
  public Parameters mergeParameters(Parameters overrideParameters) throws TzarException {
    Map<String, Object> parameters = mergeParameterMaps(asMap(), overrideParameters.asMap());
    return createParameters(parameters);
  }

  @SuppressWarnings("unchecked")
  private static <T> T castAndPut(Map<String, T> map, String key, String value) {
    return map.put(key, (T) value);
  }

  private static <T> Map<String, T> mergeParameterMaps(Map<String, T> baseParameters, Map<String, T> overrideParameters) {
    HashMap<String, T> map = Maps.newHashMap(baseParameters);
    map.putAll(overrideParameters);
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Parameters that = (Parameters) o;

    if (!parameters.equals(that.parameters)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return parameters.hashCode();
  }

  @Override
  public String toString() {
    return "Parameters{" +
        "parameters=" + parameters +
        '}';
  }
}
