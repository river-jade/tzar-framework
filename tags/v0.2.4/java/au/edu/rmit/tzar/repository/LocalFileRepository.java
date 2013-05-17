package au.edu.rmit.tzar.repository;

import java.io.File;

/**
 * A code repository which simply returns a local path. Useful for projects which
 * are not using source control.
 */
public class LocalFileRepository implements CodeRepository {
  private final File codePath;

  public LocalFileRepository(File codePath) {
    this.codePath = codePath;
  }

  @Override
  public File getModel(String revision) {
    return codePath;
  }
}
