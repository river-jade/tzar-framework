package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.YamlParser;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * Represents a location in a source repository at a given revision number.
 */
public class CodeSourceImpl implements CodeSource {
  private final URI sourceUri;
  private final String revision;
  private final RepositoryTypeImpl repositoryType;
  private final boolean forceDownload;
  private final CloseableHttpClient httpClient;

  /**
   * Constructor.
   * @param sourceUri uri specifying the location of the code / data
   * @param repositoryType type of repository containing the code / data
   * @param revision String representing the revision or empty String if this is not a versioned repository
   * @param forceDownload force this code source to re-download models, even if we already have a copy. Note that
   *                      not all Code sources will adhere to this setting.
   */
  public CodeSourceImpl(CloseableHttpClient httpClient, URI sourceUri, RepositoryTypeImpl repositoryType,
      String revision, boolean forceDownload) {
    this.sourceUri = sourceUri;
    this.repositoryType = repositoryType;
    this.revision = revision;
    this.forceDownload = forceDownload;
    this.httpClient = httpClient;
  }

  @Override
  public File getCode(File baseModelPath, String name) throws TzarException {
    return getRepository().retrieveModel(revision, name, baseModelPath);
  }

  @Override
  public ProjectSpec getProjectSpec(File baseModelPath, CodeSourceFactory codeSourceFactory, String projectFileName)
      throws TzarException, FileNotFoundException {
    if (projectFileName == null || projectFileName.length() == 0) {
      projectFileName = Constants.PROJECT_YAML;
    }
    YamlParser parser = new YamlParser();
    File file = getRepository().retrieveProjectParams(projectFileName, revision, baseModelPath);
    return parser.projectSpecFromYaml(file, codeSourceFactory);
  }

  private CodeRepository getRepository() {
    return repositoryType.createRepository(httpClient, sourceUri, forceDownload);
  }

  @Override
  public RepositoryTypeImpl getRepositoryType() {
    return repositoryType;
  }

  @Override
  public String getRevision() {
    return revision;
  }

  @Override
  public URI getSourceUri() {
    return sourceUri;
  }

  @Override
  public boolean isForceDownload() {
    return forceDownload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CodeSourceImpl that = (CodeSourceImpl) o;

    if (repositoryType != that.repositoryType) return false;
    if (!revision.equals(that.revision)) return false;
    if (!sourceUri.equals(that.sourceUri) &&
        (!Utils.makeAbsoluteUri(sourceUri.toString()).equals(Utils.makeAbsoluteUri(that.sourceUri.toString()))))
      return false;
    if (forceDownload != that.forceDownload) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sourceUri.hashCode();
    result = 31 * result + revision.hashCode();
    result = 31 * result + repositoryType.hashCode();
    result = 31 * result + (forceDownload ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CodeSource{" +
        "sourceUri='" + sourceUri + '\'' +
        ", revision='" + revision + '\'' +
        ", repositoryType=" + repositoryType +
        ", forceDownload=" + forceDownload +
        '}';
  }

  public enum RepositoryTypeImpl implements CodeSource.RepositoryType {
    LOCAL_FILE {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload) {
        return new LocalFileRepository(sourceUri);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    SVN {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload) {
        return new SvnRepository(sourceUri);
      }

      @Override
      public boolean isValidRevision(String revision) {
        try {
          Long.parseLong(revision);
          return true;
        } catch (NumberFormatException e) {
          return false;
        }
      }
    },
    GIT {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload) {
        throw new UnsupportedOperationException("Sorry, Git repository support is not yet implemented.");
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    HTTP_FILE {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload) {
        return new HttpRepository(httpClient, sourceUri, forceDownload);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    HTTP_ZIP {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload) {
        return new HttpZipRepository(httpClient, sourceUri, forceDownload);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    };

    public abstract CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean forceDownload);
    public abstract boolean isValidRevision(String revision);
  }

  public static class InvalidRevisionException extends TzarException {
    public InvalidRevisionException(String revision, RepositoryType repositoryType) {
      super(revision + " is not a valid revision for repository type: " + repositoryType);
    }
  }
}
