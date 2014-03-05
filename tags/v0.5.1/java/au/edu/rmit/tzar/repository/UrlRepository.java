package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.CodeRepository;
import au.edu.rmit.tzar.api.PathUtils;

import java.io.File;
import java.net.URI;

/**
 * Abstract base class for URL base code repositories.
 */
// TODO(river): This would be nicer as an interface with a static method,
// but that won't be available until java 8.

abstract class UrlRepository implements CodeRepository {
  protected final File baseModelsPath;
  protected final URI sourceUri;

  public UrlRepository(File baseModelsPath, URI sourceUri) {
    this.baseModelsPath = baseModelsPath;
    this.sourceUri = sourceUri;
  }

  protected static File createModelPath(String name, File baseModelsPath, URI sourceUri) {
    return new File(baseModelsPath, PathUtils.sanitiseFilename(name) + "_" + Math.abs(sourceUri.hashCode()));
  }
}
