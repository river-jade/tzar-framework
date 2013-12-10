package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.YamlParser;

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

  /**
   * Constructor.
   * @param sourceUri uri specifying the location of the code / data
   * @param repositoryType type of repository containing the code / data
   * @param revision String representing the revision or empty String if this is not a versioned repository
   */
  public CodeSourceImpl(URI sourceUri, RepositoryTypeImpl repositoryType, String revision) {
    this.sourceUri = sourceUri;
    this.repositoryType = repositoryType;
    this.revision = revision;
  }

  @Override
  public File getCode(File baseModelPath) throws TzarException {
    return getRepository(baseModelPath).retrieveModel(revision);
  }

  @Override
  public ProjectSpec getProjectSpec(File baseModelPath) throws TzarException, FileNotFoundException {
    YamlParser parser = new YamlParser();
    File file = getRepository(baseModelPath).retrieveProjectParams(Constants.PROJECT_YAML, revision);
    return parser.projectSpecFromYaml(file);
  }

  private CodeRepository getRepository(File baseModelPath) {
    return repositoryType.createRepository(sourceUri, baseModelPath);
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CodeSourceImpl that = (CodeSourceImpl) o;

    if (repositoryType != that.repositoryType) return false;
    if (!revision.equals(that.revision)) return false;
    if (!sourceUri.equals(that.sourceUri) &&
        (!Utils.makeAbsoluteUri(sourceUri.toString()).equals(Utils.makeAbsoluteUri(that.sourceUri.toString()))))
      return false;

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

  public enum RepositoryTypeImpl implements CodeSource.RepositoryType {
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
