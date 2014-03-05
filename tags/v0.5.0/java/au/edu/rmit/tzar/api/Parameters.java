package au.edu.rmit.tzar.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /**
   * Replaces any of the provided wildcard keys (as delimited by $$<param name>$$) with
   * the provided values.
   * <p/>
   * For example, if we pass in a wildcards Map containing key:base_path, and value:
   * "/foo", then if one of the parameter values of this object has name base_path
   * and value, "$$path$$/somewhere", then this method will return a Parameters
   * object containing a parameter with name: base_path, value: "/foo/somewhere".
   *
   * @param wildcards a map of wildcard names to their corresponding values
   * @return a new Parameters object with all wildcard values (in variables, inputFiles
   *         and outputFiles) replaced by the provided values.
   */
  public Parameters replaceWildcards(Map<String, String> wildcards) {
    return Parameters.createParameters(replaceWildcards(parameters, wildcards));
  }

  private static <T> Map<String, T> replaceWildcards(ImmutableMap<String, T> inputMap, Map<String, String> wildcards) {
    Map<String, T> map = Maps.newHashMap(inputMap);
    Pattern pattern = Pattern.compile("\\$\\$(.*?)\\$\\$");
    for (Map.Entry<String, T> entry : map.entrySet()) {
      if (entry.getValue() instanceof String) {
        String value = (String) entry.getValue();
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
          String wildcard = matcher.group(1);
          if (wildcards.containsKey(wildcard)) {
            // we need to escape $ and \ because these are treated as special characters in the replacement string
            String replacement = wildcards.get(wildcard).replace("\\", "\\\\").replace("$", "\\$");
            value = matcher.replaceFirst(replacement);
            matcher.reset(value);
          } else {
            LOG.warning("Found unmatched wildcard '" + wildcard + "' in parameters");
          }
        }
        // This cast to T is unavoidable, but safe because we only get here if T is castable to String.
        // We isolate it in a method so that we can suppress the warnings just on the statement containing
        // the cast.
        castAndPut(map, entry.getKey(), value);
      }
    }
    return map;
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
