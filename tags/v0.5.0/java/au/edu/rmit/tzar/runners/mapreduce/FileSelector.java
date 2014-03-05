package au.edu.rmit.tzar.runners.mapreduce;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * A mapper which selects a subset of the output files from the output directory.
 */
public class FileSelector implements Mapper {
  public static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private List<String> filenames = ImmutableList.of();
  private Map<String, String> flags;

  @Override
  public Set<File> map(File runDirectory) {
    File[] matchingFiles = runDirectory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return filenames.contains(name);
      }
    });
    return new HashSet<File>(Arrays.asList(matchingFiles));
  }

  @Override
  public void setFlags(Map<String, String> flags) {
    this.flags = flags;
    filenames = SPLITTER.splitToList(flags.get("filenames"));
  }

  @Override
  public Map<String, String> getFlags() {
    return flags;
  }
}
