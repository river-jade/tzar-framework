package au.edu.rmit.tzar.commands;

import com.beust.jcommander.IStringConverter;
import com.google.common.base.Optional;

import java.io.File;

/**
 * Used for converting optional flag values into Optional instances, rather
 * than using null.
 */
public class Converters {
  public static class OptionalString implements IStringConverter<Optional<String>> {
    @Override
    public Optional<String> convert(String value) {
      return Optional.of(value);
    }
  }

  public static class OptionalFile implements IStringConverter<Optional<File>> {
    @Override
    public Optional<File> convert(String value) {
      return Optional.of(new File(value));
    }
  }
}