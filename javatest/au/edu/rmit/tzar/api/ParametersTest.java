package au.edu.rmit.tzar.api;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

import java.io.File;
import java.util.Map;

/**
 * Test cases for Parameters class.
 */
public class ParametersTest extends TestCase {
  private Map<String, ?> variables;
  private Map<String, String> inputFiles;
  private Map<String, String> outputFiles;
  private Parameters parameters;

  @Override
  public void setUp() throws Exception {
    variables = ImmutableMap.of("a", 1, "b", 2);
    inputFiles = ImmutableMap.of("x", "foo.txt", "y", "bar.txt");
    outputFiles = ImmutableMap.of("c", "foo.gif", "d", "bar.gif");
    parameters = Parameters.createParameters(variables, inputFiles, outputFiles);
  }

  public void testGetQualifiedParams() {
    Map<String, Object> qualifiedParams = parameters.getQualifiedParams(new File("input"), new File("output"));
    assertEquals(1, qualifiedParams.get("a"));
    assertEquals(2, qualifiedParams.get("b"));
    assertEquals(new File("input", "foo.txt").getAbsolutePath(), qualifiedParams.get("x"));
    assertEquals(new File("input", "bar.txt").getAbsolutePath(), qualifiedParams.get("y"));
    assertEquals(new File("output", "foo.gif").getAbsolutePath(), qualifiedParams.get("c"));
    assertEquals(new File("output", "bar.gif").getAbsolutePath(), qualifiedParams.get("d"));
  }

  public void testGetSize() {
    assertEquals(6, parameters.getSize());
  }

  public void testDuplicateKeys() throws TzarException {
    outputFiles = ImmutableMap.of("x", "foo.gif", "y", "bar.gif");
    try {
      parameters = Parameters.createParameters(variables, inputFiles, outputFiles);
      fail("Expected TzarException to be thrown but it was not.");
    } catch (TzarException e) {
    }
  }

  public void testMergeParameters() throws TzarException {
    Parameters overrides = Parameters.createParameters(ImmutableMap.of("a", 3, "b", 2), ImmutableMap.of("x",
        "foobar.txt"), ImmutableMap.of("c", "foobar.gif"));
    Parameters result1 = parameters.mergeParameters(overrides);
    Parameters result2 = parameters.mergeParameters(overrides.getVariables(), overrides.getInputFiles(),
        overrides.getOutputFiles());
    assertEquals("Merging parameters via overload method should give same result.", result1, result2);
    assertEquals(3, result1.getVariables().get("a"));
    assertEquals(2, result1.getVariables().get("b"));
    assertEquals("foobar.txt", result1.getInputFiles().get("x"));
    assertEquals("bar.txt", result1.getInputFiles().get("y"));
    assertEquals("foobar.gif", result1.getOutputFiles().get("c"));
    assertEquals("bar.gif", result1.getOutputFiles().get("d"));
  }

  public void testReplaceWildcards() throws TzarException {
    variables = ImmutableMap.of("a", 1, "b", "$$foo$$");
    inputFiles = ImmutableMap.of("x", "$$foo$$.txt", "y", "bar.txt");
    outputFiles = ImmutableMap.of("c", "$$foo$$.gif", "d", "bar.gif");
    parameters = Parameters.createParameters(variables, inputFiles, outputFiles);
    Parameters replaced = parameters.replaceWildcards(ImmutableMap.of("foo", "hello", "gif", "png"));

    assertEquals(1, replaced.getVariables().get("a"));
    assertEquals("hello", replaced.getVariables().get("b"));
    assertEquals("hello.txt", replaced.getInputFiles().get("x"));
    assertEquals("bar.txt", replaced.getInputFiles().get("y"));
    // note that "gif" does not get replaces, as it is not surrounded by $$ $$.
    assertEquals("hello.gif", replaced.getOutputFiles().get("c"));
    assertEquals("bar.gif", replaced.getOutputFiles().get("d"));
  }
}
