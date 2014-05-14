package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.io.File;
import java.util.List;

/**
 * Unit tests for Wildcard replacer.
 */
public class WildcardReplacerTest extends TestCase {
  private static final int RUN_ID = 1234;
  private static final File OUTPUT_PATH = new File("/output/path/");
  private static final File OUTPUT_METADATA_PATH = new File(OUTPUT_PATH, "my_metadata");

  private WildcardReplacer wildcardReplacer;
  private Parameters parameters;
  private WildcardReplacer.Context context;

  public void setUp() throws Exception {
    wildcardReplacer = new WildcardReplacer();
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "value1", "param2", "value2"));
    context = new WildcardReplacer.Context(RUN_ID, new File(""), ImmutableMap.<String, File>of(), OUTPUT_PATH,
        OUTPUT_METADATA_PATH);
  }

  public void testNoReplacement() throws TzarException {
    Parameters postReplacementParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(parameters, postReplacementParameters);
  }

  public void testSimpleSingleNumberReplacement() throws TzarException {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$run_id$$", "param2",
        "value2"));
    Parameters postParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(RUN_ID, postParameters.asMap().get("param1"));
  }

  public void testDoubleNumberReplacement() throws TzarException {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$run_id$$abc$$run_id$$",
        "param2", "value2"));
    Parameters postParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(RUN_ID + "abc" + RUN_ID, postParameters.asMap().get("param1"));
  }

  public void testSimpleSingleStringReplacement() throws TzarException {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$output_path$$", "param2",
        "value2"));
    Parameters postParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(OUTPUT_PATH.getAbsolutePath() + File.separator, postParameters.asMap().get("param1"));
  }

  public void testPathEscaping() throws TzarException {
    String pathString = "foo/bar/blah";
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$path(" + pathString + ")$$",
        "param2", "value2"));
    Parameters postParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(new File("foo/bar/blah").getPath(), postParameters.asMap().get("param1"));
  }

  public void testNoReplacementInKey() throws TzarException {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1$$run_id$$", "value1", "param2",
        "value2"));
    Parameters postReplacementParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    assertEquals(parameters, postReplacementParameters);
  }

  public void testReplacementInArray() throws TzarException {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "value1", "param2",
        Lists.newArrayList(123, "abc", "$$run_id$$", "abc$$run_id$$def")));
    Parameters postReplacementParameters = wildcardReplacer.replaceWildcards(this.parameters, context);
    ImmutableMap<String, Object> map = postReplacementParameters.asMap();
    List<Object> list = (List<Object>) map.get("param2");
    assertEquals(Lists.<Object>newArrayList(123, "abc", RUN_ID, "abc" + RUN_ID + "def"), list);
  }

  public void testUnknownWildcard() {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$unknown$$"));
    try {
      wildcardReplacer.replaceWildcards(this.parameters, context);
      fail("Expected an exception");
    } catch (TzarException e) {
      assertTrue(e.getCause() instanceof WildcardReplacer.WildcardParseException);
    }
  }

  public void testWildcardWithUnexpectedParam() {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$run_id(abc)$$"));
    try {
      wildcardReplacer.replaceWildcards(this.parameters, context);
      fail("Expected an exception");
    } catch (TzarException e) {
      assertTrue(e.getCause() instanceof WildcardReplacer.WildcardParseException);
    }
  }

  public void testWildcardWithMissingParam() {
    parameters = Parameters.createParameters(ImmutableMap.<String, Object>of("param1", "$$path$$"));
    try {
      wildcardReplacer.replaceWildcards(this.parameters, context);
      fail("Expected an exception");
    } catch (TzarException e) {
      assertTrue(e.getCause() instanceof WildcardReplacer.WildcardParseException);
    }
  }

}
