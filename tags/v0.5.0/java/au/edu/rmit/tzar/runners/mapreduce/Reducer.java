package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.TzarException;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Reducer interface. TODO(river): document this properly.
 */
public interface Reducer {
  Set<File> reduce(Set<File> accumulated, Set<File> update, File outputPath) throws TzarException;

  void setFlags(Map<String, String> flags);

  Map<String,String> getFlags();
}
