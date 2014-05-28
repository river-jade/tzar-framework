package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.Repetitions;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.parser.beans.ProjectSpecBean;
import au.edu.rmit.tzar.parser.beans.RepetitionsBean;
import com.google.common.base.Preconditions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.math.BigDecimal;

/**
 * Yaml parser / writer. Handles loading project specifications from yaml files, and writing yaml files
 * from ProjectSpec objects.
 */
public class YamlParser {
  private final DumperOptions options;

  public YamlParser() {
    options = new DumperOptions();
    options.setAllowReadOnlyProperties(true);
  }

  public void parametersToYaml(Parameters parameters, File file) throws IOException {
    objectToYaml(parameters.asMap(), file);
  }

  /**
   * Loads a project specification from a YAML file containing a project spec.
   *
   * @param file the yaml file
   * @return a newly constructed and populated ProjectSpec object
   * @throws java.io.FileNotFoundException if the file does not exist
   * @throws TzarException          if the file cannot be parsed
   */
  public ProjectSpec projectSpecFromYaml(File file) throws FileNotFoundException, TzarException {
    Yaml yaml = new Yaml(new ProjectSpecConstructor());
    return objectFromYaml(file, ProjectSpecBean.class, yaml).toProjectSpec();
  }

  /**
   * Serialise a project spec to yaml and write to file.
   *
   * @param spec the project spec to serialise
   * @param file the file to write to
   * @throws java.io.IOException if the file already exists, or there is a problem writing to the file
   */
  public void projectSpecToYaml(ProjectSpecImpl spec, File file) throws IOException {
    objectToYaml(ProjectSpecBean.fromProjectSpec(spec), file);
  }

  public Repetitions repetitionsFromYaml(File repetitionsFile) throws TzarException, FileNotFoundException {
    Preconditions.checkNotNull(repetitionsFile);
    Yaml yaml = new Yaml(new Constructor(RepetitionsBean.class));
    return objectFromYaml(repetitionsFile, RepetitionsBean.class, yaml).toRepetitions();
  }

  public Repetitions repetitionsFromYaml(String repetitionsYaml) throws TzarException {
    Yaml yaml = new Yaml(new Constructor(RepetitionsBean.class));
    return objectFromYaml(repetitionsYaml, RepetitionsBean.class, yaml).toRepetitions();
  }

  private void objectToYaml(Object obj, File file) throws IOException {
    if (file.exists()) {
      throw new IOException("Cannot write YAML over existing file:" + file);
    }
    FileWriter writer = new FileWriter(file);
    try {
      Yaml yaml = new Yaml(options);
      yaml.setBeanAccess(BeanAccess.FIELD);
      writer.append(yaml.dumpAs(obj, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
    } finally {
      writer.close();
    }
  }

  private <T> T objectFromYaml(File file, Class<T> aClass, Yaml yaml) throws FileNotFoundException, TzarException {
    yaml.setBeanAccess(BeanAccess.FIELD);
    return yaml.loadAs(new FileReader(file), aClass);
  }

  /**
   * Special Constructor for deserialising parameters. It's required because SnakeYAML doesn't know
   * how to construct BigDecimals.
   */
  private class ProjectSpecConstructor extends Constructor {
    public ProjectSpecConstructor() {
      super(ProjectSpecBean.class);
      yamlConstructors.put(Tag.FLOAT, new DecimalConstruct());
    }

    private class DecimalConstruct extends Constructor.ConstructScalar {
      @Override
      public Object construct(Node nnode) {
        return new BigDecimal(((ScalarNode) nnode).getValue());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T objectFromYaml(String yamlStr, Class<T> aClass, Yaml yaml) throws TzarException {
    yaml.setBeanAccess(BeanAccess.FIELD);
    Object obj = yaml.load(yamlStr);
    if (obj.getClass() == aClass) {
      return (T) obj;
    } else {
      throw new TzarException("Yaml load resulted in unexpected type: " + obj.getClass());
    }
  }

}
