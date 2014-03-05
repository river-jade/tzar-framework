package au.edu.rmit.tzar.api;

import java.io.File;
import java.util.Set;

/**
 * Created by michaell on 20/12/2013.
 */
public interface MapReduce {
  Set<File> execute(Iterable<Run> runs, File runsetOutputPath) throws TzarException;
}
