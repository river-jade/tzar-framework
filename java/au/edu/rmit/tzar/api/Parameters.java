package au.edu.rmit.tzar.api;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
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

  public Parameters(Map<String, Object> variables, Map<String, String> inputFiles,
      Map<String, String> outputFiles) {
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
  public Parameters mergeParameters(Parameters overrideParameters) {
    Map<String, Object> variables = mergeParameters(getVariables(), overrideParameters.getVariables());
    Map<String, String> inputFiles = mergeParameters(getInputFiles(),
        overrideParameters.getInputFiles());
    Map<String, String> outputFiles = mergeParameters(getOutputFiles(),
        overrideParameters.getOutputFiles());
    return new Parameters(variables, inputFiles, outputFiles);
  }

  // TODO(michaell): javadocs
  public Parameters mergeParameters(Map<String, Object> variables, Map<String, String> inputFiles,
      Map<String, String> outputFiles) {
    return mergeParameters(new Parameters(variables, inputFiles, outputFiles));
  }

  /**
   * Writes this set of parameters out to a json file.
   *
   * @param file the file to write the json to
   * @throws java.io.IOException if the file already exists or cannot be written to
   */
  public void toJson(File file) throws IOException {
    JsonParser parser = new JsonParser();
    parser.parametersToJson(this, file);
  }

  /**
   * Replaces any of the provided wildcard keys (as delimited by $$) with
   * the provided values.
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
}
