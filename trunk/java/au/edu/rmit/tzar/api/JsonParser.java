package au.edu.rmit.tzar.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Json parser / writer. Handles loading project specifications from json files, and writing json files
 * from ProjectSpec objects.
 */
public class JsonParser {
  private static final Logger LOG = Logger.getLogger(JsonParser.class.getName());
  private final Gson gson;

  @SuppressWarnings("unchecked")
  public JsonParser() {
    gson = new GsonBuilder()
        .setPrettyPrinting()
            // the below type adapter is to handle the variable types (ie numbers, strings, and booleans
            // which we have in our parameters maps.
        .registerTypeAdapter(Object.class, new JsonDeserializer<Object>() {
          @Override
          public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
              throws JsonParseException {
            JsonPrimitive value = json.getAsJsonPrimitive();
            if (value.isBoolean()) {
              return value.getAsBoolean();
            } else if (value.isNumber()) {
              return value.getAsNumber();
            } else {
              return value.getAsString();
            }
          }
        })
            // the below type adapter is to handle the fact that out Parameters class is composed of ImmutableMaps.
            // gson doesn't know how to handle ImmutableMaps, as it's standard operation is to call no-args constructor
            // and add key/value pairs one-by-one. ImmutableMaps don't work like this, so we have to do it by hand.
            // we do it because we just lurrve immutability :)
        .registerTypeAdapter(ImmutableMap.class, new JsonDeserializer<ImmutableMap>() {
          @Override
          public ImmutableMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
              throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
            ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<String, Object>();
            for (Map.Entry<String, JsonElement> entry : entries) {
              builder.put(entry.getKey(), context.deserialize(entry.getValue(), Object.class));
            }
            return builder.build();
          }
        })
        .create();
  }

  /**
   * Loads parameters from a JSON file containing just parameters.
   *
   * @param file the json file
   * @return a newly constructed and populated Parameters object
   * @throws FileNotFoundException if the file does not exist
   * @throws RdvException          if the file cannot be parsed
   */
  public Parameters parametersFromJson(File file) throws FileNotFoundException, RdvException {
    return objectFromJson(file, Parameters.class);
  }

  public void parametersToJson(Parameters parameters, File file) throws IOException {
    if (file.exists()) {
      throw new IOException("Cannot write parameters spec over already existing file.");
    }
    FileWriter writer = new FileWriter(file);
    try {
      gson.toJson(parameters, writer);
    } finally {
      writer.flush();
      writer.close();
    }
  }

  /**
   * Loads a project specification from a JSON file containing a project spec.
   *
   * @param file the json file
   * @return a newly constructed and populated ProjectSpec object
   * @throws FileNotFoundException if the file does not exist
   * @throws RdvException          if the file cannot be parsed
   */
  public ProjectSpec projectSpecFromJson(File file) throws FileNotFoundException, RdvException {
    return objectFromJson(file, ProjectSpec.class);
  }

  /**
   * Serialise a project spec to json and write to file.
   *
   * @param spec the project spec to serialise
   * @param file the file to write to
   * @throws IOException if the file already exists, or there is a problem writing to the file
   */
  public void projectSpecToJson(ProjectSpec spec, File file) throws IOException {
    if (file.exists()) {
      throw new IOException("Cannot write Project spec over already existing file.");
    }
    FileWriter writer = new FileWriter(file);
    try {
      gson.toJson(spec, writer);
    } finally {
      writer.flush();
      writer.close();
    }
  }

  public Repetitions repetitionsFromJson(File repetitionsFile) throws RdvException, FileNotFoundException {
    return objectFromJson(repetitionsFile, Repetitions.class);
  }

  private <T> T objectFromJson(File file, Class<T> aClass) throws FileNotFoundException, RdvException {
    try {
      return gson.fromJson(new FileReader(file), aClass);
    } catch (JsonParseException e) {
      LOG.log(Level.WARNING, "Error parsing JSON file: " + file, e);
      throw new RdvException(e);
    }
  }
}
