package au.edu.rmit.tzar.api;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Test cases for Parameters class.
 */
public class ParametersTest extends TestCase {
  private Map<String, ?> variables;
  private Parameters parameters;

  @Override
  public void setUp() throws Exception {
    variables = ImmutableMap.of("a", 1, "b", 2);
    parameters = Parameters.createParameters(variables);
  }

  public void testGetSize() {
    assertEquals(2, parameters.getSize());
  }

  public void testMergeParameters() throws TzarException {
    Parameters overrides = Parameters.createParameters(ImmutableMap.of("a", 3, "b", 2));
    Parameters result1 = parameters.mergeParameters(overrides);
    Parameters result2 = parameters.mergeParameters(Parameters.createParameters(overrides.asMap()
    ));
    assertEquals("Merging parameters via overload method should give same result.", result1, result2);
    assertEquals(3, result1.asMap().get("a"));
    assertEquals(2, result1.asMap().get("b"));
  }

  public void testReplaceWildcards() throws TzarException {
    variables = ImmutableMap.of("a", 1, "b", "$$foo$$");
    parameters = Parameters.createParameters(variables);
    Parameters replaced = parameters.replaceWildcards(ImmutableMap.of("foo", "hello", "gif", "png"));

    assertEquals(1, replaced.asMap().get("a"));
    assertEquals("hello", replaced.asMap().get("b"));
  }

  public void testReplaceWildcardsWithSpecialCharacters() {
    variables = ImmutableMap.of("a", 1, "b", "$$foo$$");
    parameters = Parameters.createParameters(variables);
    Parameters replaced = parameters.replaceWildcards(ImmutableMap.of("foo", "\\abc$$\\"));

    assertEquals("\\abc$$\\", replaced.asMap().get("b"));
  }
}
