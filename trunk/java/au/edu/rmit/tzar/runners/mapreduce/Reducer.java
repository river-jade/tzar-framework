package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * A Reducer is a class that can combine a set of files (each from a single run) into a
 * single output file.
 */
public interface Reducer {
  /**
   * The reduce method takes a Set of files as input, and a final output directory path.
   * The set of files passed to reduce should be of the same form, as generated by different
   * runs, and should have the same filename. Implementations of this method should somehow
   * combine these results. See the Concatenator implementation for a simple example.
   *
   * @param input the set of input files to reduce
   * @param outputPath the path to write the output file to
   * @param filename the name of the files in the input list (they should all have the same name)
   * @return the 'reduced' output file
   * @throws TzarException
   */
  File reduce(Set<File> input, File outputPath, String filename) throws TzarException;

  void setFlags(Map<String, String> flags);

  Map<String,String> getFlags();
}