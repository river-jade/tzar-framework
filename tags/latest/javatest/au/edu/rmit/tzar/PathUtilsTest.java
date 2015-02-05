package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.PathUtils;
import junit.framework.TestCase;

import java.io.File;

/**
 * Created by michaell on 23/12/2013.
 */
public class PathUtilsTest extends TestCase {

  public void testPathCombine() {
    assertEquals("aaa" + File.separator + "bbb" + File.separator + "ccc",
        PathUtils.combine("aaa", "bbb", "ccc"));
  }

  public void testPathCombineAndReplace() {
    assertEquals("a_a_a" + File.separator + "b_b_b" + File.separator + "ccc",
        PathUtils.combineAndReplaceWhitespace("_", "a a a", "b b b", "ccc"));
  }

  public void testFilenameSanitser() {
    assertEquals("abcdefABCDEF123456", PathUtils.sanitiseFilename("abcdefABCDEF123456"));
    assertEquals("abcdef___ABCDEF123456", PathUtils.sanitiseFilename("abcdef___ABCDEF123456"));
    assertEquals("___abcdefABCDEF123456", PathUtils.sanitiseFilename("*&^abcdefABCDEF123456"));
    assertEquals("abcdef___ABCDEF123456", PathUtils.sanitiseFilename("abcdef*&^ABCDEF123456"));
    assertEquals("abcdefABCDEF123456___", PathUtils.sanitiseFilename("abcdefABCDEF123456*&^"));
  }
}
