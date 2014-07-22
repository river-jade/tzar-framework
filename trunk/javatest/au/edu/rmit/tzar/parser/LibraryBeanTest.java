package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.parser.beans.LibraryBean;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests parsing of the libraries stanza in project.yaml
 */
public class LibraryBeanTest extends TestCase {

  private static final String REVISION = "revision_x";
  public static String LIBRARY_NAME = "a_library";
  public static String LIBRARY_NAME2 = "a_library_too";
  private URI sourceUri;
  private CloseableHttpClient mockHttpClient;
  private CodeSourceFactory mockCodeSourceFactory;

  public void setUp() throws Exception {
    sourceUri = new URI("abcdef");
    mockHttpClient = mock(CloseableHttpClient.class);
    mockCodeSourceFactory = mock(CodeSourceFactory.class);
    super.setUp();
  }

  public void testLibraryToBeanAndBack() throws Exception {
    CodeSourceImpl.RepositoryTypeImpl repositoryType = CodeSourceImpl.RepositoryTypeImpl.HTTP_FILE;
    CodeSourceImpl codeSource = new CodeSourceImpl(mockHttpClient, sourceUri, repositoryType, REVISION,
        true);

    when(mockCodeSourceFactory.createCodeSource(REVISION, repositoryType, Utils.makeAbsoluteUri(sourceUri.toString()),
        true)).thenReturn(codeSource);

    LibraryBean libraryBean = LibraryBean.fromLibrary(LIBRARY_NAME, codeSource);
    Map<String, CodeSourceImpl> codeSourceMap = LibraryBean.toLibraries(Lists.newArrayList(libraryBean),
        mockCodeSourceFactory);
    assertEquals(1, codeSourceMap.size());
    assertEquals(codeSource, codeSourceMap.get(LIBRARY_NAME));
  }

  public void testsLibrariesToBeanAndBack() throws Exception {
    CodeSourceImpl.RepositoryTypeImpl httpFile = CodeSourceImpl.RepositoryTypeImpl.HTTP_FILE;
    CodeSourceImpl.RepositoryTypeImpl httpZip = CodeSourceImpl.RepositoryTypeImpl.HTTP_ZIP;

    CodeSourceImpl codeSource = new CodeSourceImpl(mockHttpClient, sourceUri, httpFile, REVISION, true);
    CodeSourceImpl codeSource2 = new CodeSourceImpl(mockHttpClient, sourceUri, httpZip, REVISION + "2", false);

    when(mockCodeSourceFactory.createCodeSource(REVISION, httpFile, Utils.makeAbsoluteUri(sourceUri.toString()),
        true)).thenReturn(codeSource);

    when(mockCodeSourceFactory.createCodeSource(REVISION + "2", httpZip, Utils.makeAbsoluteUri(sourceUri.toString()),
        false)).thenReturn(codeSource2);

    ImmutableMap<String, CodeSourceImpl> map = ImmutableMap.of(LIBRARY_NAME, codeSource, LIBRARY_NAME2, codeSource2);
    List<LibraryBean> libraryBeans = LibraryBean.fromLibraries(map);
    Map<String, CodeSourceImpl> codeSourceMap = LibraryBean.toLibraries(libraryBeans, mockCodeSourceFactory);
    assertEquals(2, codeSourceMap.size());
    assertEquals(codeSource, codeSourceMap.get(LIBRARY_NAME));
    assertEquals(codeSource2, codeSourceMap.get(LIBRARY_NAME2));
  }
}
