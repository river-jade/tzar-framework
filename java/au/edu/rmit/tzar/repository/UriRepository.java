package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.CodeRepository;

import java.io.File;
import java.net.URI;

/**
 * Abstract base class for URI base code repositories.
 */
abstract class UriRepository implements CodeRepository {
  protected final File modelPath;
  protected final URI sourceUri;

  public UriRepository(File baseModelsPath, URI sourceUri) {
    modelPath = createModelPath(baseModelsPath, sourceUri);
    this.sourceUri = sourceUri;
  }

  protected static File createModelPath(File baseModelsPath, URI sourceUri) {
    return new File(baseModelsPath, sourceUri.toString().replaceAll("[/ :]+", "_"));
  }
}
