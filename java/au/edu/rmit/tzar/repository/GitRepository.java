package au.edu.rmit.tzar.repository;

import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Files;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

/**
 * A representation of the repository for different compiled versions of the model code.
 */
public class GitRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(GitRepository.class.getName());


  /**
   * Constructor.
   *
   * @param sourceUri      uri to the project location in the subversion repository
   */
//  public GitRepository(URI sourceUri) {
//    this(sourceUri, SVNClientManager.newInstance().getUpdateClient(),
//        SVNClientManager.newInstance().getWCClient());
//  }
//
  /**
   * Constructor.
   *
   * @param sourceUri      uri to the project location in the subversion repository
   * @param updateClient   SVNUpdateClient object (for updating the local svn client)
   * @param wcClient       SVNWCClient object (for cleaning up the local svn client)
   */
  GitRepository(URI sourceUri) {
    super(sourceUri);
//    this.updateClient = updateClient;
//    this.wcClient = wcClient;
//    DAVRepositoryFactory.setup();
  }

  @Override
  public File retrieveModel(String revision, String name, File baseModelPath) throws TzarException {
    File modelPath = createModelPath(name, baseModelPath, sourceUri);
    LOG.info(String.format("Retrieving code revision: %s, to %s", revision, modelPath));

    Git result;
    try {
      result = Git.cloneRepository()
              .setURI(sourceUri.toString())
              .setDirectory(baseModelPath)
              .call();
    } catch (GitAPIException e) {
      throw new TzarException(e);
    }
    return result.getRepository().getDirectory();
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision, File destPath) throws TzarException {
    File tempDir = Files.createTempDir();
    LOG.fine("Retrieving project params at revision: " + revision + ", to local path:" + tempDir);

    try {
      Git result = Git.cloneRepository()
          .setURI(sourceUri.toString())
          .setDirectory(tempDir)
          .call();
//      updateClient.doExport(url.appendPath(projectParamFilename, false), tempDir, svnRevision, svnRevision, null, true,
//          SVNDepth.EMPTY);
      return new File(tempDir, projectParamFilename);
//    } catch (SVNException e) {
//      throw new TzarException("Error retrieving projectparams from source control.", e);
    } catch (InvalidRemoteException e) {
      e.printStackTrace();
    } catch (TransportException e) {
      e.printStackTrace();
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
    return tempDir;
  }

  @Override
  public String getHeadRevision() throws TzarException {
//    try {
//      SVNRepository repository = SVNRepositoryFactory.create(getUrl());
//      repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
//      return Long.toString(repository.getLatestRevision());
//    } catch (SVNException e) {
//      throw new TzarException("Couldn't retrieve the latest revision from SVN.", e);
//    }
    return null;
  }

//  private static SVNRevision parseSvnRevision(String revision) throws TzarException {
//    try {
//      return SVNRevision.create(Long.parseLong(revision));
//    } catch (NumberFormatException ex) {
//      throw new TzarException("Revision:" + revision + " was not a number.", ex);
//    }
//  }

//  private SVNURL getUrl() throws SVNException {
//    return SVNURL.parseURIEncoded(sourceUri.toString());
//  }
}
