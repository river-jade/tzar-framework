package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A mapper which simply copies a single file to another file
 * with a different name. Effectively used for renaming.
 */
public class FileCopier implements Mapper {
  private Map<String, String> flags;
  private String sourceFile;

  public FileCopier() {
  }

  public FileCopier(String sourceFile, String destFile) {
    this.sourceFile = sourceFile;
    this.destFile = destFile;
  }

  private String destFile;

  @Override
  public Set<File> map(File runDirectory) throws TzarException {
    HashSet<File> returnSet = new HashSet<File>();

    for (File file : runDirectory.listFiles()) {
      if (file.getName().equals(sourceFile)) {
        try {
          File dest = new File(runDirectory, destFile);

          Files.copy(file, dest);
          returnSet.add(dest);
          return returnSet;
        } catch (IOException e) {
          throw new TzarException(e);
        }
      }
    }
    return returnSet;
  }

  @Override
  public void setFlags(Map<String, String> flags) {
    this.flags = flags;
    sourceFile = flags.get("source_filename");
    destFile = flags.get("dest_filename");
  }

  @Override
  public Map<String, String> getFlags() {
    return flags;
  }
}
