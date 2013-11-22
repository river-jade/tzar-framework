package au.edu.rmit.tzar;

import com.google.common.base.Optional;
import junit.framework.TestCase;
import org.python.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Unit tests for the Utilities class
 */
public class UtilsTest extends TestCase {
  /**
   * Tests that the copyDirectory method works as expected with the
   * RegexFilter.
   */
  public void testCopyWithFilter() throws IOException {
    File dest = Files.createTempDir();
    testCopyWithFilter("foo", dest);
    assertFalse(new File(dest, "foobar").exists());
    assertFalse(new File(dest, "barfoo").exists());
    assertTrue(new File(dest, "foo").exists());
    assertFalse(new File(dest, "bar").exists());

    testCopyWithFilter("foo.*", dest);
    assertTrue(new File(dest, "foobar").exists());
    assertFalse(new File(dest, "barfoo").exists());
    assertTrue(new File(dest, "foo").exists());
    assertFalse(new File(dest, "bar").exists());

  }

  private void testCopyWithFilter(String filter, File dest) throws IOException {
    File source = Files.createTempDir();

    new File(source, "foobar").createNewFile();
    new File(source, "barfoo").createNewFile();
    new File(source, "foo").createNewFile();
    new File(source, "bar").createNewFile();

    Utils.copyDirectory(source, dest, new Utils.NoopRenamer(), Utils.RegexFilter.of(Optional.of(filter)));
  }

  public void testRecursiveDelete() throws IOException {
    File source = Files.createTempDir();

    new File(source, "foobar").createNewFile();
    new File(source, "barfoo").createNewFile();
    File foo = new File(source, "foo");
    foo.mkdirs();
    new File(foo, "bar").createNewFile();
    Utils.deleteRecursively(source);
    assertFalse(source.exists());
  }

  public void testMakeAbsoluteUri() throws URISyntaxException {
    assertEquals(new URI("file:///abc/def/ghi"), Utils.makeAbsoluteUri("/abc/def/ghi"));
    assertEquals(new URI("file:///abc/def/ghi"), Utils.makeAbsoluteUri("file:///abc/def/ghi"));
    assertEquals(new URI("http://abc/def/ghi"), Utils.makeAbsoluteUri("http://abc/def/ghi"));
  }

  public void testPathCombine() {
    assertEquals("aaa" + File.separator + "bbb" + File.separator + "ccc",
        Utils.Path.combine("aaa", "bbb", "ccc"));
  }

  public void testPathCombineAndReplace() {
    assertEquals("a_a_a" + File.separator + "b_b_b" + File.separator + "ccc",
        Utils.Path.combineAndReplaceWhitespace("_", "a a a", "b b b", "ccc"));
  }
}
