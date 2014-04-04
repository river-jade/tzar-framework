package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Collection;
import java.util.Map;
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
    // this multimap maps filenames to the list of files (one from each run) for that filename.
    Multimap<String, File> multiMap = ArrayListMultimap.create();

    for (Run run : runs) {
      Set<File> mappedFiles = mapper.map(new File(runsetOutputPath, run.getRunDirectoryName()));
      for (File file : mappedFiles) {
        multiMap.put(file.getName(), file);
      }
    }

    // outputFiles is a set of files, one for each of the uniquely named input files
    Set<File> outputFiles = Sets.newHashSet();
    for (Map.Entry<String, Collection<File>> entry : multiMap.asMap().entrySet()) {
      outputFiles.add(reducer.reduce(ImmutableSet.copyOf(entry.getValue()), runsetOutputPath, entry.getKey()));
    }
    return outputFiles;
  }

  public Mapper getMapper() {
    return mapper;
  }
  public Reducer getReducer() {
    return reducer;
  }
}
