package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;

import java.io.File;

/**
 * Returns a file path for a given code revision. Implementations may choose to ignore
 * the revision number if not relevant.
 */
public interface CodeRepository {
  File getModel(String revision) throws TzarException;
}
