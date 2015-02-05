package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.runners.mapreduce.*;

/**
 * Provides a simpler way to instantiate a concatenating mapreduce
 * in the project.yaml.
 */
public class ConcatenateBean {
  boolean heading_row;
  String input_filename;
  String output_filename;

  public MapReduce toMapReduce() throws TzarException {
    Mapper mapper = new FileCopier(input_filename, output_filename);
    Reducer reducer = new Concatenator(heading_row);
    return new MapReduce(mapper, reducer);
  }
}
