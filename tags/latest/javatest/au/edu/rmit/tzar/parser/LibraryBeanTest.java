package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.parser.beans.LibraryBean;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Tests parsing of the libraries stanza in project.yaml
 */
public class LibraryBeanTest extends TestCase {

  private static final String REVISION = "revision_x";
  public static String LIBRARY_NAME = "a_library";
  public static String LIBRARY_NAME2 = "a_library_too";
  private URI sourceUri;

  public void setUp() throws Exception {
    sourceUri = new URI("abcdef");
    super.setUp();
  }

  public void testLibraryToBeanAndBack() throws Exception {
    CodeSourceImpl codeSource = new CodeSourceImpl(sourceUri, CodeSourceImpl.RepositoryTypeImpl.HTTP_FILE, REVISION,
        true);
    LibraryBean libraryBean = LibraryBean.fromLibrary(LIBRARY_NAME, codeSource);
    Map<String, CodeSourceImpl> codeSourceMap = LibraryBean.toLibraries(Lists.newArrayList(libraryBean));
    assertEquals(1, codeSourceMap.size());
    assertEquals(codeSource, codeSourceMap.get(LIBRARY_NAME));
  }

  public void testsLibrariesToBeanAndBack() throws Exception {
    CodeSourceImpl codeSource = new CodeSourceImpl(sourceUri, CodeSourceImpl.RepositoryTypeImpl.HTTP_FILE, REVISION,
        true);
    CodeSourceImpl codeSource2 = new CodeSourceImpl(sourceUri, CodeSourceImpl.RepositoryTypeImpl.HTTP_ZIP,
        REVISION + "2", false);
    ImmutableMap<String, CodeSourceImpl> map = ImmutableMap.of(LIBRARY_NAME, codeSource, LIBRARY_NAME2, codeSource2);
    List<LibraryBean> libraryBeans = LibraryBean.fromLibraries(map);
    Map<String, CodeSourceImpl> codeSourceMap = LibraryBean.toLibraries(libraryBeans);
    assertEquals(2, codeSourceMap.size());
    assertEquals(codeSource, codeSourceMap.get(LIBRARY_NAME));
    assertEquals(codeSource2, codeSourceMap.get(LIBRARY_NAME2));
  }
}
