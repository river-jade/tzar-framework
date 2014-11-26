package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Files;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

/**
 * A representation of the repository for different compiled versions of the model code.
 */
public class SvnRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(SvnRepository.class.getName());

  private final SVNUpdateClient updateClient;
  private final SVNWCClient wcClient;

  // TODO(river): these are a hack, and will break under concurrent thread use. Find a nicer /
  // threadsafe way to do this.
  private static SVNRevision lastRevision;
  private static URI lastSourceUri;

  /**
   * Constructor.
   *
   * @param sourceUri      uri to the project location in the subversion repository
   */
  public SvnRepository(URI sourceUri) {
    this(sourceUri, SVNClientManager.newInstance().getUpdateClient(),
        SVNClientManager.newInstance().getWCClient());
  }

  /**
   * Constructor.
   *
   * @param sourceUri      uri to the project location in the subversion repository
   * @param updateClient   SVNUpdateClient object (for updating the local svn client)
   * @param wcClient       SVNWCClient object (for cleaning up the local svn client)
   */
  SvnRepository(URI sourceUri, SVNUpdateClient updateClient, SVNWCClient wcClient) {
    super(sourceUri);
    this.updateClient = updateClient;
    this.wcClient = wcClient;
    DAVRepositoryFactory.setup();
  }

  @Override
  public File retrieveModel(String revision, String name, File baseModelPath) throws TzarException {
    File modelPath = createModelPath(name, baseModelPath, sourceUri);
    LOG.info(String.format("Retrieving code revision: %s, to %s", revision, modelPath));
    try {
      SVNURL url = getUrl();
      SVNRevision svnRevision = parseSvnRevision(revision);

      if (svnRevision.equals(lastRevision) && sourceUri.equals(lastSourceUri)) {
        LOG.info(String.format("Model already exists in local SVN client at correct version with path %s so not " +
                "downloading", modelPath));
        return modelPath;
      }

      // we do a cleanup here because otherwise, if an update is aborted part way through (by sigkill for instance),
      // the local repository is left in a bad state, and future updates all fail.
      if (modelPath.exists()) {
        wcClient.doCleanup(modelPath);
      }
      updateClient.doCheckout(url, modelPath, svnRevision, svnRevision, SVNDepth.INFINITY, true);
      lastRevision = svnRevision;
      lastSourceUri = sourceUri;
      return modelPath;
    } catch (SVNException e) {
      throw new TzarException("Error retrieving model from SVN", e);
    }
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision, File destPath) throws TzarException {
    File tempDir = Files.createTempDir();
    LOG.fine("Retrieving project params at revision: " + revision + ", to local path:" + tempDir);

    try {
      SVNURL url = getUrl();
      SVNRevision svnRevision = parseSvnRevision(revision);
      updateClient.doExport(url.appendPath(projectParamFilename, false), tempDir, svnRevision, svnRevision, null, true,
          SVNDepth.EMPTY);
      return new File(tempDir, projectParamFilename);
    } catch (SVNException e) {
      throw new TzarException("Error retrieving projectparams from source control.", e);
    }
  }

  @Override
  public String getHeadRevision() throws TzarException {
    try {
      SVNRepository repository = SVNRepositoryFactory.create(getUrl());
      repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
      return Long.toString(repository.getLatestRevision());
    } catch (SVNException e) {
      throw new TzarException("Couldn't retrieve the latest revision from SVN.", e);
    }
  }

  private static SVNRevision parseSvnRevision(String revision) throws TzarException {
    try {
      return SVNRevision.create(Long.parseLong(revision));
    } catch (NumberFormatException ex) {
      throw new TzarException("Revision:" + revision + " was not a number.", ex);
    }
  }

  private SVNURL getUrl() throws SVNException {
    return SVNURL.parseURIEncoded(sourceUri.toString());
  }
}
