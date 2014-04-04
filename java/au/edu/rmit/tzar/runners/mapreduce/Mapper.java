package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the mapper function. Mapper maps from a run output directory to
 * a set of file paths. It may do any required transformation as required, including creating new files,
 * though it's recommended that the Mapper does not overwrite existing output files.
 */
public interface Mapper {
  /**
   * Given a fully qualified run output directory for a single run, this method does
   * any required work with the files in that directory, including potentially writing more files to
   * the output directory, or to a temporary location, and returns a Set of files for the reduce phase.
   * @param runDirectory
   * @return
   */
  Set<File> map(File runDirectory) throws TzarException;

  void setFlags(Map<String, String> flags);

  Map<String,String> getFlags();
}
