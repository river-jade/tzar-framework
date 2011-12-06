package au.edu.rmit.tzar.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for repetition generators, which generate a list of values for
 * a provided key.
 */
public abstract class RepetitionGenerator<T> {
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

  /**
   * Types of Generators. New implementations of this class should be added to this list so
   * that they can be correctly deserialised.
   */
  public enum GeneratorType {
    LINEAR_STEP("linear_step"),
    NORMAL_DISTRIBUTION("normal_distribution");

    public static final Map<String, GeneratorType> TYPES = new HashMap<String, GeneratorType>();
    
    static {
      for (GeneratorType type : GeneratorType.values()) {
        TYPES.put(type.name, type);
      }
    }

    private final String name;

    GeneratorType(String name) {
      this.name = name;
    }
  }
}
