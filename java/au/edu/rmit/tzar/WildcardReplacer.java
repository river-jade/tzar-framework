package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for replacing parameter wildcards with runtime values.
 */
public class WildcardReplacer {
  private static WildcardFunction RUN_ID = new SimpleWildcardFunction() {
    public Object apply(Context context) {
      return context.runId;
    }
  };

  private static WildcardFunction PATH = new ValueWildcardFunction() {
    public String apply(String value, Context context) {
      return new File(value).getPath();
    }
  };

  private static WildcardFunction MODEL_PATH = new SimpleWildcardFunction() {
    public String apply(Context context) {
      return context.model.getAbsolutePath() + File.separator;
    }
  };

  private static WildcardFunction LIBRARY_PATH = new ValueWildcardFunction() {
    public String apply(String value, Context context) {
      return context.libraries.get(value).getAbsolutePath() + File.separator;
    }
  };

  private static WildcardFunction OUTPUT_PATH = new SimpleWildcardFunction() {
    public String apply(Context context) {
      return context.outputPath.getAbsolutePath() + File.separator;
    }
  };

  private static WildcardFunction OUTPUT_METADATA_PATH = new SimpleWildcardFunction() {
    public String apply(Context context) {
      return context.outputMetadataPath.getAbsolutePath() + File.separator;
    }
  };

  private static ImmutableMap<String, WildcardFunction> WILDCARDS = ImmutableMap.<String, WildcardFunction>builder()
      .put("run_id", RUN_ID)
      .put("path", PATH)

      .put("model_path", MODEL_PATH)
      .put("library_path", LIBRARY_PATH)
      .put("output_path", OUTPUT_PATH)
      .put("output_metadata_path", OUTPUT_METADATA_PATH)
      .build();

  // regex matches $$abc(def)$$ and $$abc$$, capturing 'abc' and 'def' for the first case and 'abc' for the second.
  private static final Pattern PATTERN = Pattern.compile(
      "\\$\\$" + // starting $$
      "([^\\s\\($]+)" + // wildcard name (cannot contain whitespace, open paren, or '$').
      "(?:\\((\\S+)\\))?" + // optional wildcard value inside ()
      "\\$\\$" // ending $$
  );

  /**
   * Creates a new Parameters object based on the provided Parameters, with all wildcards in parameter values
   * substituted. Wildcards are opened by $$ and closed by $$. Example wildcards are: $$run_id$$, or
   * $$library_path(xyz)$$.
   *
   * Simple wildcards such as $$run_id$$ are simply replaced by the appropriate value (ie runId in this case).
   * Value wildcards such as $$library_path(xyz)$$ involve a lookup based on the value in parentheses (xyz) in this
   * case.
   *
   * For example, a parameter that has value xxx$$run_id$$xxx will have its value replaced with
   * @param params the parameters (potentially) containing wildcards to be replaced. Note that this object is not
   *               modified.
   * @return a new Parameters object with the wildcards replaced.
   * @throws TzarException
   */
  public Parameters replaceWildcards(Parameters params, Context context) throws TzarException {
    Map<String, Object> map = Maps.newHashMap();

    for (Map.Entry<String, Object> entry : params.asMap().entrySet()) {
      String parameterName = entry.getKey();
      try {
        Object replacement = getReplacement(entry.getValue(), context);
        map.put(parameterName, replacement);
      } catch (WildcardParseException e) {
        throw new TzarException(String.format("Couldn't parse wildcard in parameter: %s. Error was: %s", parameterName,
            e.getMessage()), e);
      }
    }
    return Parameters.createParameters(map);
  }

  private static Object getReplacement(Object value, Context context) throws WildcardParseException {
    if (value instanceof String) {
      return replaceInString(context, value.toString());
    } else if (value instanceof List) {
      List<Object> finalList = Lists.newArrayList();
      for (Object eachVal : (List)value) {
        Object eachReplacement = getReplacement(eachVal, context);
        finalList.add(eachReplacement);
      }
      return finalList;
    } else {
      return value; // Not a string or a list, so no replacement
    }
  }

  private static Object replaceInString(Context context, String value) throws WildcardParseException {
    Matcher matcher = PATTERN.matcher(value.toString());
    if (matcher.matches()) { // Entire value matches. We handle this as a special case, so that we can avoid
      // converting numbers etc to Strings.
      return applyWildcardFunction(matcher.group(1), matcher.group(2), context);
    } else {
      matcher.reset(); // need to reset the matcher, as we've already called matcher.matches().
      StringBuilder sb = new StringBuilder();
      int position = 0;

      while (matcher.find()) {
        Object replacement = applyWildcardFunction(matcher.group(1), matcher.group(2), context);

        sb.append(value.toString().substring(position, matcher.start()));
        sb.append(replacement);
        position = matcher.end();
      }
      sb.append(value.toString().substring(position));
      return sb.toString();
    }
  }

  /**
   * Looks up the appropriate wildcard function for the given wildcard name, and applies the function
   * to the wildcard value (which may be null, depending on the wildcard).
   *
   * @param wildcardName the name of the wildcard
   * @param wildcardValue the wildcard value
   * @param context execution context
   *
   * @return the result of applying the wildcard function
   * @throws WildcardParseException if the wildcard is not recognised, or is in the wrong form (eg has a parameter
   * when it should not).
   */
  private static Object applyWildcardFunction(String wildcardName, String wildcardValue, Context context)
      throws WildcardParseException {
    if (!WILDCARDS.containsKey(wildcardName)) {
      throw new WildcardParseException(String.format("Wildcard '%s' not recognised.", wildcardName));
    }
    WildcardFunction wildcardFunction = WILDCARDS.get(wildcardName);
    return wildcardFunction.apply(Optional.fromNullable(wildcardValue), context);
  }

  /**
   * A Function interface for handling conversion of wildcards into their concrete values.
   */
  public interface WildcardFunction {
    /**
     * @param value the optional value of the wildcard parameter (eg for $$path(/abc/def/)$$, value would be
     *              equal to "/abc/def/". For simple wildcards, that is those without a wildcard parameter, this
     *              will be unset.
     * @param context context for executing the function
     * @return
     */
    Object apply(Optional<String> value, Context context) throws WildcardParseException;
  }

  /**
   * Abstract base class for wildcard functions that do not take a parameter, eg $$run_id$$.
   */
  private static abstract class SimpleWildcardFunction implements WildcardFunction {
    public abstract Object apply(Context context);

    @Override
    public final Object apply(Optional<String> value, Context context) throws WildcardParseException {
      if (value.isPresent()) {
        throw new WildcardParseException("Did not expect a value for wildcard.");
      }
      return apply(context);
    }
  }

  /**
   * Abstract base class for wildcard functions that do take a parameter, eg $$path(/abc/def/)$$.
   */
  private static abstract class ValueWildcardFunction implements WildcardFunction {
    @Override
    public final Object apply(Optional<String> value, Context context) throws WildcardParseException {
      if (!value.isPresent()) {
        throw new WildcardParseException("Expected a value for wildcard.");
      }
      return apply(value.get(), context);
    }

    protected abstract String apply(String value, Context context);
  }

  /**
   * Simple context object for the replaceWildcards call.
   */
  public static class Context {
    private final File model;
    private final ImmutableMap<String, File> libraries;
    public final int runId;
    public final File outputPath;
    public final File outputMetadataPath;

    /**
     * @param runId the id of the run
     * @param model the path to the model code for the run
     * @param libraries a map of library names to their paths for this run
     * @param outputPath the path for the run output files
     * @param outputMetadataPath
     */
    public Context(int runId, File model, ImmutableMap<String, File> libraries, File outputPath,
        File outputMetadataPath) {
      this.runId = runId;
      this.model = model;
      this.libraries = libraries;
      this.outputPath = outputPath;
      this.outputMetadataPath = outputMetadataPath;
    }
  }

  static class WildcardParseException extends Exception {
    public WildcardParseException(String message) {
      super(message);
    }
  }
}
