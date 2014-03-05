package au.edu.rmit.tzar.runners.mapreduce;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the mapper function. Mapper maps from a run output directory to
 * a set of file paths. It may do any required transformation as required, though
 * it's recommended that the Mapper does not overwrite existing output files.
 */
public interface Mapper {
  Set<File> map(File runDirectory);

  void setFlags(Map<String, String> flags);

  Map<String,String> getFlags();
}
