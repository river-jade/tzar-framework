package au.edu.rmit.tzar.api;

import au.edu.rmit.tzar.Utils;
import com.google.common.base.Function;

import java.io.File;
import java.util.Arrays;

/**
 * Utility functions for dealing with file paths.
 */
public class PathUtils {
  /**
   * Combine a list of String paths in a platform independent way.
   * @param paths
   * @return
   */
  public static String combine(String... paths) {
    return Utils.reduce(Arrays.asList(paths), new Function<Utils.Pair<File, String>, File>() {
      public File apply(Utils.Pair<File, String> input) {
        return new File(input.first, input.second);
      }
    }, null).getPath();
  }

  /**
   * Combine a list of String paths in a platform independent way, also replacing any whitespace
   * in the provided paths with the provided replacement character.
   * @param paths the paths to join
   * @param replacementChar the character to use to replace any whitespace characters
   * @return
   */
  public static String combineAndReplaceWhitespace(final String replacementChar, String... paths) {
    return Utils.reduce(Arrays.asList(paths), new Function<Utils.Pair<File, String>, File>() {
      public File apply(Utils.Pair<File, String> input) {
        return new File(input.first, input.second.replaceAll("\\W", replacementChar));
      }
    }, null).getPath();
  }
}
