package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.RdvException;
import com.google.common.annotations.VisibleForTesting;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.util.logging.Logger;

/**
 * A representation of the repository for different compiled versions of the model code.
 */
public class SvnRepository implements CodeRepository {
  private static final Logger LOG = Logger.getLogger(SvnRepository.class.getName());

  private final SVNUpdateClient updateClient;
  private final SVNWCClient wcClient;
  private final String svnUrl;
  private final File modelsPath;

  /**
   * Constructor.
   *
   * @param svnUrl         url to the subversion repository
   * @param baseModelsPath path to the local directory in which to put the
   */
  public SvnRepository(String svnUrl, File baseModelsPath) {
    this(svnUrl, baseModelsPath, SVNClientManager.newInstance().getUpdateClient(),
        SVNClientManager.newInstance().getWCClient());
  }

  /**
   * Constructor.
   *
   * @param svnUrl         url to the subversion repository
   * @param baseModelsPath path to the local directory in which to put the
   *                       svn client (will be created if it doesn't exist)
   * @param updateClient   SVNUpdateClient object (for updating the local svn client)
   * @param wcClient       SVNWCClient object (for cleaning up the local svn client)
   */
  SvnRepository(String svnUrl, File baseModelsPath, SVNUpdateClient updateClient, SVNWCClient wcClient) {
    this.svnUrl = svnUrl;
    modelsPath = createModelPath(baseModelsPath, svnUrl);
    this.updateClient = updateClient;
    this.wcClient = wcClient;
    DAVRepositoryFactory.setup();
  }

  /**
   * Checks out from subversion repository into a local directory at the provided revision
   * number.
   *
   * @param revision the version of the model / framework to load
   * @return the path to the cached model / framework code
   * @throws RdvException if an error occurs contacting the svn repository
   */
  @Override
  public File getModel(String revision) throws RdvException {
    LOG.info("Retrieving code revision: " + revision + ", to " + modelsPath);
    try {
      SVNURL url = SVNURL.parseURIEncoded(svnUrl);
      SVNRevision svnRevision;
      try {
        svnRevision = SVNRevision.create(Long.parseLong(revision));
      } catch (NumberFormatException e) {
        if ("head".equalsIgnoreCase(revision)) {
          svnRevision = SVNRevision.HEAD;
        } else {
          throw new RdvException("Unrecognised revision: '" + revision + "'. Must be 'head' or an integer.");
        }
      }
      // we do a cleanup here because otherwise, if an update is aborted part way through (by sigkill for instance),
      // the local repository is left in a bad state, and future updates all fail.
      if (modelsPath.exists()) {
        wcClient.doCleanup(modelsPath);
      }
      updateClient.doCheckout(url, modelsPath, svnRevision, svnRevision, SVNDepth.INFINITY, true);
      return modelsPath;
    } catch (SVNException e) {
      throw new RdvException("Error retrieving model from SVN", e);
    }
  }

  @VisibleForTesting
  static File createModelPath(File baseModelsPath, String svnUrl) {
    return new File(baseModelsPath, svnUrl.replaceAll("[/ :]", "_"));
  }
}
