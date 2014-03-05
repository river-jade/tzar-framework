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
    // relative path
    assertEquals(new URI("file:" + new File("abc/def/ghi").getAbsolutePath()), Utils.makeAbsoluteUri("abc/def/ghi"));
    // absolute path
    String path = String.format("%1$sabc%1$sdef%1$sghi", File.separator);
    assertEquals(new URI("file:" + new File(path).getAbsolutePath()),
        Utils.makeAbsoluteUri(path));
    // file uri
    assertEquals(new URI("file:///abc/def/ghi"), Utils.makeAbsoluteUri("file:///abc/def/ghi"));
    // http uri
    assertEquals(new URI("http://abc/def/ghi"), Utils.makeAbsoluteUri("http://abc/def/ghi"));
  }
}
