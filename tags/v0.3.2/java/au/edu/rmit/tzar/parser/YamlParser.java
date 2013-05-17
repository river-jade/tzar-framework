package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Scenario;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

  /**
   * Loads parameters from a YAML file containing just parameters.
   *
   * @param file the yaml file
   * @return a newly constructed and populated Parameters object
   * @throws java.io.FileNotFoundException if the file does not exist
   * @throws TzarException          if the file cannot be parsed
   */
  public Parameters parametersFromYaml(File file) throws FileNotFoundException, TzarException {
    if (file == null) {
      return Parameters.EMPTY_PARAMETERS;
    }
    Yaml yaml = new Yaml(new ProjectSpecConstructor());
    return objectFromYaml(file, ParametersBean.class, yaml).toParameters();
  }

  public void parametersToYaml(Parameters parameters, File file) throws IOException {
    objectToYaml(ParametersBean.fromParameters(parameters), file);
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
    ProjectSpec projectSpec = objectFromYaml(file, ProjectSpecBean.class, yaml).toProjectSpec();
    projectSpec.validate();
    return projectSpec;
  }

  /**
   * Serialise a project spec to yaml and write to file.
   *
   * @param spec the project spec to serialise
   * @param file the file to write to
   * @throws java.io.IOException if the file already exists, or there is a problem writing to the file
   */
  public void projectSpecToYaml(ProjectSpec spec, File file) throws IOException {
    objectToYaml(ProjectSpecBean.fromProjectSpec(spec), file);
  }

  public Repetitions repetitionsFromYaml(File repetitionsFile) throws TzarException, FileNotFoundException {
    if (repetitionsFile == null) {
      return Repetitions.EMPTY_REPETITIONS;
    }
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

  private static class ProjectSpecBean {
    private ParametersBean base_params;
    private List<ScenarioBean> scenarios;
    private String project_name;
    
    public static ProjectSpecBean fromProjectSpec(ProjectSpec spec) {
      ProjectSpecBean bean = new ProjectSpecBean();
      bean.base_params = ParametersBean.fromParameters(spec.getBaseParams());
      bean.scenarios = ScenarioBean.fromScenarios(spec.getScenarios());
      bean.project_name = spec.getProjectName();
      return bean;
    }

    public ProjectSpec toProjectSpec() {
      return new ProjectSpec(project_name, base_params.toParameters(), ScenarioBean.toScenarios(scenarios));
    }
  }

  private static class ParametersBean {
    private Map<String, Object> variables;
    private Map<String, String> input_files;
    private Map<String, String> output_files;

    public static ParametersBean fromParameters(Parameters parameters) {
      ParametersBean bean = new ParametersBean();
      bean.input_files = parameters.getInputFiles();
      bean.output_files = parameters.getOutputFiles();
      bean.variables = parameters.getVariables();
      return bean;
    }

    public Parameters toParameters() {
      try {
        return Parameters.createParameters(
            variables == null ? ImmutableMap.<String, Object>of() : variables,
            input_files == null ? ImmutableMap.<String, String>of() : input_files,
            output_files == null ? ImmutableMap.<String, String>of() : output_files);
      } catch (TzarException e) {
        throw new YAMLException("Couldn't parse parameters.", e);
      }
    }
  }

  private static class ScenarioBean {
    private String name;
    private ParametersBean parameters;
    
    public static List<ScenarioBean> fromScenarios(List<Scenario> scenarios) {
      return Lists.transform(scenarios, new Function<Scenario, ScenarioBean>() {
        @Override
        public ScenarioBean apply(Scenario scenario) {
          return ScenarioBean.fromScenario(scenario);
        }
      });
    }

    public static ScenarioBean fromScenario(Scenario scenario) {
      ScenarioBean bean = new ScenarioBean();
      bean.name = scenario.getName();
      bean.parameters = ParametersBean.fromParameters(scenario.getParameters());
      return bean;
    }

    public static List<Scenario> toScenarios(List<ScenarioBean> scenarios) {
      return Lists.transform(scenarios, new Function<ScenarioBean, Scenario>() {
        @Override
        public Scenario apply(ScenarioBean bean) {
          return bean.toScenario();
        }
      });
    }

    private Scenario toScenario() {
      return new Scenario(name, parameters.toParameters());
    }
  }

  private static class RepetitionsBean {
    private List<ParametersBean> repetitions;
    private List<RepetitionGeneratorBean> generators;
    
    public Repetitions toRepetitions() {
      List<Parameters> parametersList = repetitions == null ? null :
          Lists.transform(repetitions, new Function<ParametersBean, Parameters>() {
        @Override
        public Parameters apply(ParametersBean bean) {
          return bean == null ? Parameters.EMPTY_PARAMETERS : bean.toParameters();
        }
      });

      List<RepetitionGenerator<?>> repetitionGenerators = generators == null ? null : Lists.transform(generators,
          new Function<RepetitionGeneratorBean, RepetitionGenerator<?>>() {
        @Override
        public RepetitionGenerator<?> apply(RepetitionGeneratorBean bean) {
          return bean == null ? null : bean.toGenerator();
        }
      });
      return new Repetitions(parametersList, repetitionGenerators);
    }
  }

  private static class RepetitionGeneratorBean {
    private String generator_type;
    private String key;
    private BigDecimal start;
    private int count;
    private BigDecimal step_size;
    private BigDecimal mean;
    private BigDecimal std_dev;

    public RepetitionGenerator<?> toGenerator() {
      switch (RepetitionGenerator.GeneratorType.TYPES.get(generator_type)) {
        case LINEAR_STEP:
          return new LinearStepGenerator(key, start, count, step_size);
        case NORMAL_DISTRIBUTION:
          return new NormalDistributionGenerator(key, mean, count, std_dev);
        default:
          throw new YAMLException("Generator type: " + generator_type + " not recognised.");
      }
    }
  }
}
