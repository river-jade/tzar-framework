package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.CodeRepository;
import au.edu.rmit.tzar.api.PathUtils;

import java.io.File;
import java.net.URI;

/**
 * Abstract base class for URL base code repositories.
 */
abstract class UrlRepository implements CodeRepository {
  protected final File baseModelsPath;
  protected final URI sourceUri;

  public UrlRepository(File baseModelsPath, URI sourceUri) {
    this.baseModelsPath = baseModelsPath;
    this.sourceUri = sourceUri;
  }

  protected File createModelPath(String name) {
    return new File(baseModelsPath, PathUtils.sanitiseFilename(name) + "_" + Math.abs(sourceUri.hashCode()));
  }
}
