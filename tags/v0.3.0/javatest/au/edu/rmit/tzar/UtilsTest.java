package au.edu.rmit.tzar;

import junit.framework.TestCase;
import org.python.google.common.io.Files;

import java.io.File;
import java.io.IOException;

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

    Utils.copyDirectory(source, dest, new Utils.NoopRenamer(), new Utils.RegexFilter(filter));
  }
}
