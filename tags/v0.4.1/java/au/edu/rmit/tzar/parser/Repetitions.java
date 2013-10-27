package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

/**
 * Class to hold repetitions definition. A repetitions object contains a list of Parameters object,
 * each one of which represents one repetition. For that repetition, the parameters in that Parameter
 * object override the parameters given by the scenario, project spec or global spec.
 */
public class Repetitions {
  private final List<Parameters> staticRepetitions;

  private final List<RepetitionGenerator<?>> generators;

  public static final Repetitions EMPTY_REPETITIONS = new Repetitions(Lists.newArrayList(Parameters.EMPTY_PARAMETERS),
      new ArrayList<RepetitionGenerator<?>>());

  public Repetitions(List<Parameters> staticRepetitions, List<RepetitionGenerator<?>> generators) {
    this.staticRepetitions = staticRepetitions == null ? new ArrayList<Parameters>() : staticRepetitions;
    this.generators = generators == null ? new ArrayList<RepetitionGenerator<?>>() : generators;
  }

  /**
   * Gets the list of Parameters (where each parameters object itself represents a list of parameters)
   * for the repetitions defined by this object. The list of Parameters is generated by computing the
   * cartesian product of: the set of "static" repetitions held by this object and the set of
   * all repetitions generated by all of the generators held by this object.
   *
   * @return a list of Parameters objects
   */
  public List<Parameters> getParamsList() throws TzarException {
    // Convert the list of key-value pairs generated by each generator into a List of Sets of KeyValuePairs for
    // consumption by the Guava cartesianProduct function.
    List<Set<KeyValuePair>> keyValuePairsList = Lists.transform(getGenerators(), new Function<RepetitionGenerator<?>,
        Set<KeyValuePair>>() {
      @Override
      public Set<KeyValuePair> apply(final RepetitionGenerator<?> generator) {
        return new LinkedHashSet<KeyValuePair>(generateKeyValuePairs(generator));
      }
    });

    Set<List<KeyValuePair>> cartesianProduct = Sets.cartesianProduct(keyValuePairsList);
    List<Parameters> generatedParamsList = cartesianProductToParametersList(cartesianProduct);

    List<Parameters> mergedParameters = Lists.newArrayList();
    List<Parameters> staticRepetitions = getStaticRepetitions();
    if (staticRepetitions.isEmpty()) {
      return generatedParamsList;
    } else {
      for (Parameters staticParams : staticRepetitions) {
        for (Parameters generatedParams : generatedParamsList) {
          mergedParameters.add(staticParams.mergeParameters(generatedParams));
        }
      }
      return mergedParameters;
    }
  }

  public List<RepetitionGenerator<?>> getGenerators() {
    return generators;
  }

  public List<Parameters> getStaticRepetitions() {
    return staticRepetitions;
  }

  /**
   * Executes the provided generator and puts the output into a List of KeyValuePairs.
   *
   * @param generator generator to generate the values
   * @return List of key value pairs from the generator
   */
  private static List<KeyValuePair> generateKeyValuePairs(final RepetitionGenerator<?> generator) {
    return Lists.transform(generator.generate(), new Function<Object, KeyValuePair>() {
      @Override
      public Repetitions.KeyValuePair apply(Object value) {
        return new Repetitions.KeyValuePair(generator.getKey(), value);
      }
    });
  }

  /**
   * Convenience function to convert a Set of Lists of KeyValuePairs (which is the output from
   * the Guava Maps.cartesianProductFunction -- hence the function name) into a List of Parameters
   * objects.
   */
  private static List<Parameters> cartesianProductToParametersList(Set<List<KeyValuePair>> cartesianProduct)
      throws TzarException {
    List<Parameters> parametersList = Lists.newArrayList();
    for (List<KeyValuePair> keyValuePairs : cartesianProduct) {
      parametersList.add(Parameters.createParameters(keyValuePairsToMap(keyValuePairs),
          Maps.<String, String>newHashMap(), Maps.<String, String>newHashMap()));
    }
    return parametersList;
  }

  /**
   * Converts a List of KeyValuePairs into a map.
   */
  private static Map<String, Object> keyValuePairsToMap(List<KeyValuePair> keyValuePairs) {
    Map<String, Object> map = Maps.newHashMap();
    for (KeyValuePair pair : keyValuePairs) {
      map.put(pair.key, pair.value);
    }
    return map;
  }

  private static final class KeyValuePair {
    private final String key;
    private final Object value;

    private KeyValuePair(String key, Object value) {
      this.key = key;
      this.value = value;
    }
  }
}
