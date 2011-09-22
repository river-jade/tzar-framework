package au.edu.rmit.tzar.api;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;

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
  public static final Parameters EMPTY_PARAMETERS = new Parameters(ImmutableMap.<String, Object>of(),
      ImmutableMap.<String, String>of(),
      ImmutableMap.<String, String>of());

  @SerializedName("variables")
  private final ImmutableMap<String, Object> variables;
  @SerializedName("input_files")
  private final ImmutableMap<String, String> inputFiles;
  @SerializedName("output_files")
  private final ImmutableMap<String, String> outputFiles;

  /**
   * Factory method to create a new Parameters object. Verifies that there are no duplicate keys.
   *
   * @param variables
   * @param inputFiles
   * @param outputFiles
   * @return a new Parameters object
   * @throws RdvException if there are duplicate keys
   */
  public static Parameters createParameters(Map<String, ?> variables, Map<String, String> inputFiles,
      Map<String, String> outputFiles) throws RdvException {
    checkForDuplicateKeys(variables, inputFiles);
    checkForDuplicateKeys(variables, outputFiles);
    checkForDuplicateKeys(inputFiles, outputFiles);
    return new Parameters(variables, inputFiles, outputFiles);
  }

  /**
   * Private constructor. Key names across the maps must be unique. Callers of this method should call validate
   * immediately after, unless there are guaranteed to be no duplicate keys.
   */
  private Parameters(Map<String, ?> variables, Map<String, String> inputFiles, Map<String, String> outputFiles) {
    this.variables = ImmutableMap.copyOf(variables);
    this.inputFiles = ImmutableMap.copyOf(inputFiles);
    this.outputFiles = ImmutableMap.copyOf(outputFiles);
  }

  /**
   * Returns a map of keys to parameters (variables, input_files and output_files),
   * where the input and output filenames are qualified
   * by the provided baseInputPath and baseOutputPaths respectively.
   *
   * @param baseInputPath  the absolute base path for input files
   * @param baseOutputPath the absolute base path for output files
   * @return a Map of qualified parameters
   */
  public Map<String, Object> getQualifiedParams(File baseInputPath, File baseOutputPath) {
    Map<String, Object> qualifiedParams = Maps.newLinkedHashMap(variables);
    qualifiedParams.putAll(prependPath(baseInputPath, inputFiles));
    qualifiedParams.putAll(prependPath(baseOutputPath, outputFiles));
    return qualifiedParams;
  }

  /**
   * Map of keys to values of type String, Integer, Boolean or BigDecimal.
   */
  public ImmutableMap<String, Object> getVariables() {
    return variables != null ? variables : ImmutableMap.<String, Object>of();
  }

  /**
   * Map of keys to input file names. These can be qualified by an input file path
   * using getQualifiedParams.
   */
  public ImmutableMap<String, String> getInputFiles() {
    return inputFiles != null ? inputFiles : ImmutableMap.<String, String>of();
  }

  /**
   * Map of keys to output file names. These can be qualified by an output file path
   * using getQualifiedParams.
   */
  public ImmutableMap<String, String> getOutputFiles() {
    return outputFiles != null ? outputFiles : ImmutableMap.<String, String>of();
  }

  /**
   * Gets the total number of parameters defined.
   *
   * @return total number of parameters
   */
  public int getSize() {
    return getOutputFiles().size() + getInputFiles().size() + getVariables().size();
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
  public Parameters mergeParameters(Parameters overrideParameters) throws RdvException {
    Map<String, Object> variables = mergeParameters(getVariables(), overrideParameters.getVariables());
    Map<String, String> inputFiles = mergeParameters(getInputFiles(),
        overrideParameters.getInputFiles());
    Map<String, String> outputFiles = mergeParameters(getOutputFiles(),
        overrideParameters.getOutputFiles());
    return createParameters(variables, inputFiles, outputFiles);
  }

  // TODO(michaell): javadocs
  public Parameters mergeParameters(Map<String, Object> variables, Map<String, String> inputFiles,
      Map<String, String> outputFiles) throws RdvException {
    return mergeParameters(createParameters(variables, inputFiles, outputFiles));
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
    return new Parameters(replaceWildcards(variables, wildcards),
        replaceWildcards(inputFiles, wildcards),
        replaceWildcards(outputFiles, wildcards));
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
            value = matcher.replaceFirst(wildcards.get(wildcard));
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

  private static void checkForDuplicateKeys(Map<String, ?> map1, Map<String, ?> map2) throws RdvException {
    Sets.SetView<String> intersection = Sets.intersection(map1.keySet(), map2.keySet());
    if (!intersection.isEmpty()) {
      throw new RdvException("Duplicate keys found. There cannot be any shared keys between variables, " +
          "input_files and output_files. Duplicate keys are: " + intersection);
    }
  }

  private static <T> Map<String, T> mergeParameters(Map<String, T> baseParameters, Map<String, T> overrideParameters) {
    HashMap<String, T> map = Maps.newHashMap(baseParameters);
    map.putAll(overrideParameters);
    return map;
  }

  private static Map<String, String> prependPath(final File baseFileName, Map<String, String> inputFiles) {
    Function<String, String> appendFileName = new Function<String, String>() {
      @Override
      public String apply(String filename) {
        return new File(baseFileName, filename).getAbsolutePath();
      }
    };
    return Maps.transformValues(inputFiles, appendFileName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Parameters that = (Parameters) o;

    if (inputFiles != null ? !inputFiles.equals(that.inputFiles) : that.inputFiles != null) return false;
    if (outputFiles != null ? !outputFiles.equals(that.outputFiles) : that.outputFiles != null) return false;
    if (variables != null ? !variables.equals(that.variables) : that.variables != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = variables != null ? variables.hashCode() : 0;
    result = 31 * result + (inputFiles != null ? inputFiles.hashCode() : 0);
    result = 31 * result + (outputFiles != null ? outputFiles.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Parameters{" +
        "variables=" + variables +
        "\ninputFiles=" + inputFiles +
        "\noutputFiles=" + outputFiles +
        '}';
  }
}
