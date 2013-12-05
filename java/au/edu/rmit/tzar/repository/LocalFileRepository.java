package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.CodeRepository;
import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

/**
 * A code repository which simply returns a local path. Useful for projects which
 * are not using source control.
 */
public class LocalFileRepository implements CodeRepository {
  private static final Logger LOG = Logger.getLogger(LocalFileRepository.class.getName());

  private final File modelPath;

  public LocalFileRepository(URI sourceUri) {
    modelPath = new File(sourceUri);
  }

  @Override
  public File retrieveModel(String revision) {
    return modelPath;
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision) throws TzarException {
    return new File(modelPath, projectParamFilename);
  }

  @Override
  public String getHeadRevision() throws TzarException {
    return ""; // file systems are not generally revisioned.
  }
}
