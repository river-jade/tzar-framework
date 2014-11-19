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
 */
public class Concatenator implements Reducer {
  private static final Logger LOG = Logger.getLogger(Concatenator.class.getName());
  private boolean headingRow;
  private Map<String, String> flags;

  public Concatenator() {
  }

  public Concatenator(boolean headingRow) {
    this.headingRow = headingRow;
  }

  @Override
  public File reduce(Set<File> input, File outputPath, String filename) throws TzarException {
    try {
      File outputFile = new File(outputPath, filename);
      boolean fileExists = outputFile.exists();

      if (fileExists) {
        LOG.info(String.format("Concatenator output file %s exists from previous job. Appending.", outputFile));
      } else {
        if (!outputFile.createNewFile()) {
          throw new IOException("Couldn't create new file: " + outputFile);
        }
      }

      boolean newFile = !fileExists;
      for (File updateFile : input) {
        // TODO(river): note that using updateFile.getName means we'll flatten the output hierarchy. Unfortunately,
        // this is hard to avoid. Once we move to java 7, we'll have access to Path.relativize, which will mean
        // we can fix this more easily. For now, we just deal with the limitation, which should be fine for most cases.

        appendFile(outputFile, updateFile, newFile);
        newFile = false;
      }

      return outputFile;
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  @Override
  public void setFlags(Map<String, String> flags) {
    this.flags = flags;
    headingRow = Boolean.parseBoolean(flags.get("heading_row"));
  }

  @Override
  public Map<String, String> getFlags() {
    return flags;
  }

  private void appendFile(File outputFile, File inputFile, boolean newFile) throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
      BufferedReader reader = new BufferedReader(new FileReader(inputFile));
      closer.register(writer);
      closer.register(reader);

      String line;
      boolean firstRow = true;
      while ((line = reader.readLine()) != null) {
        if (firstRow) {
          firstRow = false;
          if (headingRow && !newFile) {
            // there's a heading row, and this isn't a new file so skip the first row
            continue;
          }
        }
        writer.write(line);
        writer.newLine();
      }
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }
}
