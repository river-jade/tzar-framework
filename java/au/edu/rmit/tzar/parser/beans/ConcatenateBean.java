package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.runners.mapreduce.*;
import com.google.common.collect.Lists;

/**
 * Provides a simpler way to instantiate a concatenating mapreduce
 * in the project.yaml.
 */
public class ConcatenateBean {
  boolean heading_row;
  String input_filename;
  String output_filename;

  public MapReduce toMapReduce() throws TzarException {
    Mapper mapper = new FileSelector(Lists.newArrayList(input_filename));
    Reducer reducer = new Concatenator(heading_row, output_filename);
    return new MapReduce(mapper, reducer);
  }
}
