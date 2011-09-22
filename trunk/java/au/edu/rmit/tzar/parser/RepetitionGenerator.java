package au.edu.rmit.tzar.parser;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for repetition generators, which generate a list of values for
 * a provided key.
 */
public abstract class RepetitionGenerator<T> {
  @SerializedName("key")
  private final String key;

  public RepetitionGenerator(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  /**
   * Generates the list of values for this generator.
   *
   * @return list of values
   */
  public abstract List<T> generate();

  public static final Map<String, Class<? extends RepetitionGenerator>> TYPES =
      new HashMap<String, Class<? extends RepetitionGenerator>>() {{
        put("linear_step", LinearStepGenerator.class);
        put("normal_distribution", NormalDistributionGenerator.class);
      }};
}
