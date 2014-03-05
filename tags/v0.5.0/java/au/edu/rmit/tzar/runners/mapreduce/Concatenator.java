package au.edu.rmit.tzar.runners.mapreduce;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Closer;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A reducer which concatenates output files together, optionally skipping the first row (for
 * the case of csv files with heading row).
 * TODO(river): optionally use the heading row from the first file.
 */
public class Concatenator implements Reducer {
  private static final Logger LOG = Logger.getLogger(Concatenator.class.getName());
  private boolean skipFirstRow;
  private Map<String, String> flags;

  @Override
  public Set<File> reduce(Set<File> accumulated, Set<File> update, File outputDirectory) throws TzarException {
    try {
      for (File updateFile : update) {
        // TODO(river): note that using updateFile.getName means we'll flatten the output hierarchy. Unfortunately,
        // this is hard to avoid. Once we move to java 7, we'll have access to Path.relativize, which will mean
        // we can fix this more easily. For now, we just deal with the limitation, which should be fine for most cases.
        File outputFile = new File(outputDirectory, updateFile.getName());
        boolean newFile = accumulated.add(outputFile);

        if (outputFile.exists()) {
          if (newFile) {
            LOG.warning(String.format("Concatenator output file %s exists from previous job. Appending.", outputFile));
          }
        } else {
          if (!outputFile.createNewFile()) {
            throw new IOException("Couldn't create new file: " + outputFile);
          }
        }
        appendFile(outputFile, updateFile);
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }

    return accumulated;
  }

  @Override
  public void setFlags(Map<String, String> flags) {
    this.flags = flags;
    skipFirstRow = Boolean.parseBoolean(flags.get("skipfirstrow"));
  }

  @Override
  public Map<String, String> getFlags() {
    return flags;
  }

  private void appendFile(File accumulated, File update) throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(accumulated, true));
      BufferedReader reader = new BufferedReader(new FileReader(update));
      closer.register(writer);
      closer.register(reader);

      String line;
      boolean firstRow = true;
      while ((line = reader.readLine()) != null) {
        if (!(firstRow && skipFirstRow)) {
          writer.write(line);
          writer.newLine();
        }
        firstRow = false;
      }
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }
}
