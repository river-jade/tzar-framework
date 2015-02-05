package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.parser.beans.DownloadMode;
import com.google.common.base.Objects;
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
  private final DownloadMode downloadMode;
  private final CloseableHttpClient httpClient;

  /**
   * Constructor.
   * @param sourceUri uri specifying the location of the code / data
   * @param repositoryType type of repository containing the code / data
   * @param revision String representing the revision or empty String if this is not a versioned repository
   * @param downloadMode how to cache doanloaded code / libraries
   */
  public CodeSourceImpl(CloseableHttpClient httpClient, URI sourceUri, RepositoryTypeImpl repositoryType,
      String revision, DownloadMode downloadMode) {
    this.sourceUri = sourceUri;
    this.repositoryType = repositoryType;
    this.revision = revision;
    this.downloadMode = downloadMode;
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
    boolean downloadOnce = (downloadMode == DownloadMode.ONCE);
    return repositoryType.createRepository(httpClient, sourceUri, downloadOnce);
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
  public DownloadMode getDownloadMode() {
    return downloadMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CodeSourceImpl that = (CodeSourceImpl) o;

    if (downloadMode != that.downloadMode) return false;
    if (httpClient != null ? !httpClient.equals(that.httpClient) : that.httpClient != null) return false;
    if (repositoryType != that.repositoryType) return false;
    if (revision != null ? !revision.equals(that.revision) : that.revision != null) return false;
    if (sourceUri != null ? !sourceUri.equals(that.sourceUri) : that.sourceUri != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sourceUri != null ? sourceUri.hashCode() : 0;
    result = 31 * result + (revision != null ? revision.hashCode() : 0);
    result = 31 * result + (repositoryType != null ? repositoryType.hashCode() : 0);
    result = 31 * result + (downloadMode != null ? downloadMode.hashCode() : 0);
    result = 31 * result + (httpClient != null ? httpClient.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("sourceUri", sourceUri)
        .add("revision", revision)
        .add("repositoryType", repositoryType)
        .add("downloadMode", downloadMode)
        .add("httpClient", httpClient)
        .toString();
  }

  public enum RepositoryTypeImpl implements CodeSource.RepositoryType {
    LOCAL_FILE {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
        return new LocalFileRepository(sourceUri);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    SVN {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
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
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
        throw new UnsupportedOperationException("Sorry, Git repository support is not yet implemented.");
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    HTTP_FILE {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
        return new HttpRepository(httpClient, sourceUri, downloadOnce);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    HTTP_ZIP {
      @Override
      public CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri, boolean downloadOnce) {
        return new HttpZipRepository(httpClient, sourceUri, downloadOnce);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    };

    public abstract CodeRepository createRepository(CloseableHttpClient httpClient, URI sourceUri,
        boolean downloadOnce);
    public abstract boolean isValidRevision(String revision);
  }

  public static class InvalidRevisionException extends TzarException {
    public InvalidRevisionException(String revision, RepositoryType repositoryType) {
      super(revision + " is not a valid revision for repository type: " + repositoryType);
    }
  }
}
