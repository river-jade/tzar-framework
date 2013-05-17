package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.RdvException;
import junit.framework.TestCase;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for the SvnRepository.
 */
public class SvnRepositoryTest extends TestCase {
  private static final String TEST_URL = "http://foobar.com/blah";
  private static final File BASE_MODEL_PATH = new File("/tmp/foo");
  private SVNUpdateClient mockClient;
  private SvnRepository repository;

  @Override
  public void setUp() throws Exception {
    mockClient = Mockito.mock(SVNUpdateClient.class);
    SVNWCClient mockWCClient = Mockito.mock(SVNWCClient.class);
    repository = new SvnRepository(TEST_URL, BASE_MODEL_PATH, mockClient, mockWCClient);
  }

  /**
   * Simulates retrieving the model from sourceforge.
   */
  public void testGetModel() throws SVNException, RdvException {
    String revision = "1000";

    File expectedPath = SvnRepository.createModelPath(BASE_MODEL_PATH, TEST_URL);

    File modelPath = repository.getModel(revision);

    SVNRevision svnRevision = SVNRevision.create(Long.parseLong(revision));
    verify(mockClient).doCheckout(SVNURL.parseURIEncoded(TEST_URL), expectedPath, svnRevision,
        svnRevision, SVNDepth.INFINITY, true);
    assertEquals(expectedPath, modelPath);
  }

  public void testGetModelBadRevision() {
    try {
      repository.getModel("foo");
      fail("Expected RdvException to be thrown.");
    } catch (RdvException e) {
    }
  }
}
