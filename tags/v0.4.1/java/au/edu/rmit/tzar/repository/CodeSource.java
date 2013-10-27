package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.parser.YamlParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * Represents a location in a source repository at a given revision number.
 */
public class CodeSource {
  private final URI sourceUri;
  private final String revision;
  private final RepositoryType repositoryType;

  public CodeSource(URI sourceUri, RepositoryType repositoryType, String revision) {
    this.sourceUri = sourceUri;
    this.repositoryType = repositoryType;
    this.revision = revision;
  }

  /**
   * Retrieves the code at this object's sourceUri from the repository and writes it to baseModelPath.
   * @param baseModelPath the local file path at which to write the downloaded data
   * @return the path to the downloaded code
   * @throws TzarException
   */
  public File getCode(File baseModelPath) throws TzarException {
    return getRepository(baseModelPath).retrieveModel(revision);
  }

  public ProjectSpec getProjectSpec(File baseModelPath) throws TzarException, FileNotFoundException {
    YamlParser parser = new YamlParser();
    return parser.projectSpecFromYaml(getRepository(baseModelPath).retrieveProjectParams("projectparams.yaml",
        revision));
  }

  private CodeRepository getRepository(File baseModelPath) {
    return repositoryType.createRepository(sourceUri, baseModelPath);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CodeSource that = (CodeSource) o;

    if (repositoryType != that.repositoryType) return false;
    if (!revision.equals(that.revision)) return false;
    if (!sourceUri.equals(that.sourceUri)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sourceUri.hashCode();
    result = 31 * result + revision.hashCode();
    result = 31 * result + repositoryType.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "CodeSource{" +
        "sourceUri='" + sourceUri + '\'' +
        ", revision='" + revision + '\'' +
        ", repositoryType=" + repositoryType +
        '}';
  }

  public URI getSourceUri() {
    return sourceUri;
  }

  public RepositoryType getRepositoryType() {
    return repositoryType;
  }

  public String getRevision() {
    return revision;
  }

  public enum RepositoryType {
    LOCAL_FILE {
      @Override
      public CodeRepository createRepository(URI sourceUri, File baseModelPath) {
        return new LocalFileRepository(sourceUri);
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    },
    SVN {
      @Override
      public CodeRepository createRepository(URI sourceUri, File baseModelPath) {
        return new SvnRepository(sourceUri, baseModelPath);
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
      public CodeRepository createRepository(URI sourceUri, File baseModelPath) {
        throw new UnsupportedOperationException("Sorry, Git repository support is not yet implemented.");
      }

      @Override
      public boolean isValidRevision(String revision) {
        return true;
      }
    };

    public abstract CodeRepository createRepository(URI sourceUri, File baseModelPath);

    public abstract boolean isValidRevision(String revision);
  }
}
