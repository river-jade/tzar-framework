package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Combination of a Mapper and a Reducer to apply to a set of run results.
 */
public class MapReduce implements au.edu.rmit.tzar.api.MapReduce {
  private static final Logger LOG = Logger.getLogger(MapReduce.class.getName());

  private final Mapper mapper;
  private final Reducer reducer;

  public MapReduce(Mapper mapper, Reducer reducer) {
    this.mapper = mapper;
    this.reducer = reducer;
  }

  @Override
  public Set<File> execute(Iterable<Run> runs, File runsetOutputPath) throws TzarException {
    Set<File> accumulated = Sets.newHashSet();

    for (Run run : runs) {
      Set<File> mappedFiles = mapper.map(new File(runsetOutputPath, run.getRunDirectoryName()));
      accumulated = reducer.reduce(accumulated, mappedFiles, runsetOutputPath);
    }
    return accumulated;
  }

  public Mapper getMapper() {
    return mapper;
  }
  public Reducer getReducer() {
    return reducer;
  }
}
