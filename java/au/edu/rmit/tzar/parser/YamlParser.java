package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.ArrayList;
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

  private static class ProjectSpecBean {
    private Map<String, Object> base_params;
    private List<ScenarioBean> scenarios;
    private String project_name;
    private String runner_class;
    private String runner_flags;
    private List<LibraryBean> libraries;
    private RepetitionsBean repetitions;

    public static ProjectSpecBean fromProjectSpec(ProjectSpecImpl spec) {
      ProjectSpecBean bean = new ProjectSpecBean();
      bean.base_params = spec.getBaseParams().asMap();
      bean.scenarios = ScenarioBean.fromScenarios(spec.getScenarios());
      bean.project_name = spec.getProjectName();
      bean.runner_class = spec.getRunnerClass();
      bean.runner_flags = spec.getRunnerFlags();
      bean.libraries = LibraryBean.fromLibraries(spec.getLibraries());
      bean.repetitions = RepetitionsBean.fromRepetitions(spec.getRepetitions());
      return bean;
    }

    public ProjectSpecImpl toProjectSpec() throws TzarException {
      Repetitions reps = (repetitions == null ? Repetitions.EMPTY_REPETITIONS : repetitions.toRepetitions());
      Map<String, CodeSourceImpl> libs = (libraries == null ? ImmutableMap.<String, CodeSourceImpl>of() :
          LibraryBean.toLibraries(libraries));

      List<String> errors = Lists.newArrayList();
      if (base_params == null) {
        errors.add("Base parameters must be set.");
      }
      if (project_name == null) {
        errors.add("Project name must be set.");
      }
      if (runner_class == null) {
        errors.add("Runner class must be set.");
      }
      if (!errors.isEmpty()) {
        throw new TzarException(String.format("Errors parsing project spec: [\n%s\n]", Joiner.on("\n").join(errors)));
      }

      return new ProjectSpecImpl(project_name, runner_class, Strings.nullToEmpty(runner_flags),
          Parameters.createParameters(base_params), ScenarioBean.toScenarios(scenarios), reps, libs);
    }
  }

  private static class ScenarioBean {
    private String name;
    private Map<String, Object> parameters;
    
    public static List<ScenarioBean> fromScenarios(List<Scenario> scenarios) {
      List<ScenarioBean> beans = Lists.newArrayList();
      for (Scenario scenario : scenarios) {
        beans.add(ScenarioBean.fromScenario(scenario));
      }
      return beans;
    }

    public static ScenarioBean fromScenario(Scenario scenario) {
      ScenarioBean bean = new ScenarioBean();
      bean.name = scenario.getName();
      bean.parameters = scenario.getParameters().asMap();
      return bean;
    }

    public static List<Scenario> toScenarios(List<ScenarioBean> beans) {
      if (beans == null) {
        return ImmutableList.of();
      }
      ImmutableList.Builder<Scenario> scenarios = ImmutableList.builder();
      for (ScenarioBean bean : beans) {
        scenarios.add(bean.toScenario());
      }
      return scenarios.build();
    }

    private Scenario toScenario() {
      return new Scenario(name, parameters == null ? Parameters.EMPTY_PARAMETERS :
          Parameters.createParameters(parameters));
    }
  }

  private static class LibraryBean {
    private String name;
    private String repo_type;
    private String uri;
    private String revision;
    
    public static List<LibraryBean> fromLibraries(Map<String, CodeSourceImpl> libraries) {
      List<LibraryBean> beans = Lists.newArrayList();
      for (Map.Entry<String, CodeSourceImpl> library : libraries.entrySet()) {
        beans.add(LibraryBean.fromLibrary(library));
      }
      return beans;
    }

    public static LibraryBean fromLibrary(Map.Entry<String, CodeSourceImpl> library) {
      LibraryBean bean = new LibraryBean();
      bean.name = library.getKey();
      CodeSourceImpl source = library.getValue();
      bean.repo_type = source.getRepositoryType().name().toLowerCase();
      bean.revision = source.getRevision();
      bean.uri = source.getSourceUri().toString();
      return bean;
    }

    public static Map<String, CodeSourceImpl> toLibraries(List<LibraryBean> libraryBeans) {
      Map<String, CodeSourceImpl> libraries = Maps.newHashMap();
      for (LibraryBean bean : libraryBeans) {
        libraries.put(bean.name, bean.toCodeSource());
      }
      return libraries;
    }

    private CodeSourceImpl toCodeSource() {
      return new CodeSourceImpl(Utils.makeAbsoluteUri(uri),
          CodeSourceImpl.RepositoryTypeImpl.valueOf(repo_type.toUpperCase()), revision);
    }
  }

  private static class RepetitionsBean {
    private List<Map<String, Object>> static_repetitions;
    private List<RepetitionGeneratorBean> generators;
    
    public Repetitions toRepetitions() {
      List<Parameters> parametersList;
      if (static_repetitions == null) {
        parametersList = ImmutableList.of();
      } else {
        ImmutableList.Builder<Parameters> builder = ImmutableList.builder();
        for (Map<String, Object> bean : static_repetitions) {
          builder.add(Parameters.createParameters(bean));
        }
        parametersList = builder.build();
      }

      List<RepetitionGenerator<?>> repetitionGenerators;
      if (generators == null) {
        repetitionGenerators = ImmutableList.of();
      } else {
        ImmutableList.Builder<RepetitionGenerator<?>> builder = ImmutableList.builder();
        for (RepetitionGeneratorBean bean : generators) {
          builder.add(bean.toGenerator());
        }
        repetitionGenerators = builder.build();
      }
      return new Repetitions(parametersList, repetitionGenerators);
    }

    public static RepetitionsBean fromRepetitions(Repetitions repetitions) {
      RepetitionsBean bean = new RepetitionsBean();
      bean.static_repetitions = new ArrayList<Map<String, Object>>();
      for (Parameters parameters : repetitions.getStaticRepetitions()) {
        Map<String, Object> map = parameters.asMap();
        bean.static_repetitions.add(map);
      }
      bean.generators = new ArrayList<RepetitionGeneratorBean>();
      for (RepetitionGenerator<?> generator : repetitions.getGenerators()) {
        bean.generators.add(RepetitionGeneratorBean.fromGenerators(generator));
      }
      return bean;
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

    public static RepetitionGeneratorBean fromGenerators(RepetitionGenerator<?> generator) {
      Class<? extends RepetitionGenerator> generatorClass = generator.getClass();
      final RepetitionGenerator.GeneratorType type;
      RepetitionGeneratorBean bean = new RepetitionGeneratorBean();
      if (generatorClass == LinearStepGenerator.class) {
        type = RepetitionGenerator.GeneratorType.LINEAR_STEP;
        LinearStepGenerator linearStepGenerator = (LinearStepGenerator) generator;
        bean.count = linearStepGenerator.count;
        bean.start = linearStepGenerator.start;
        bean.step_size = linearStepGenerator.stepSize;
      } else if (generatorClass == NormalDistributionGenerator.class) {
        type = RepetitionGenerator.GeneratorType.NORMAL_DISTRIBUTION;
        NormalDistributionGenerator normalDistributionGenerator = (NormalDistributionGenerator) generator;
        bean.count = normalDistributionGenerator.count;
        bean.mean = normalDistributionGenerator.mean;
        bean.std_dev = normalDistributionGenerator.stdDev;
      } else {
        throw new YAMLException("Generator type: " + generatorClass + " not recognised.");
      }
      bean.generator_type = type.toString().toLowerCase();
      bean.key = generator.getKey();
      return bean;
    }
  }
}
