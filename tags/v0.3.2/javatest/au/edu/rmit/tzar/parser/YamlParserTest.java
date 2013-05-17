package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.api.Scenario;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the YamlParser class.
 */
public class YamlParserTest extends TestCase {
  YamlParser yamlParser;

  protected void setUp() throws Exception {
    yamlParser = new YamlParser();
  }

  public void testWriteThenReadProjectSpec() throws IOException, TzarException {
    Map<String, Object> variables = Maps.newLinkedHashMap();
    variables.put("test1", 3);
    variables.put("test2", true);
    variables.put("test3", new BigDecimal("3.2")); // we use bigdecimal because floats suffer rounding issues
    Map<String, String> inputFiles = Maps.newLinkedHashMap();
    inputFiles.put("test4", "foo");
    inputFiles.put("test5", "bar");
    Map<String, String> outputFiles = Maps.newLinkedHashMap();

    Parameters baseParameters = Parameters.createParameters(variables, inputFiles, outputFiles);

    ArrayList<Scenario> scenarios = Lists.newArrayList();

    variables.put("test1", 7);
    variables.put("test2", false);
    variables.put("test6", 99);
    inputFiles.put("test4", "foo2");
    inputFiles.put("test7", "flurgle");

    Parameters overrideParameters1 = Parameters.createParameters(variables, inputFiles, outputFiles);
    Parameters overrideParameters2 = Parameters.EMPTY_PARAMETERS;
    scenarios.add(new Scenario("test scenario1", overrideParameters1));
    scenarios.add(new Scenario("test scenario2", overrideParameters2));
    ProjectSpec projectSpec = new ProjectSpec("test project", baseParameters, scenarios);

    File tempFile = File.createTempFile("yaml_parser_test", null);
    tempFile.delete();
    yamlParser.projectSpecToYaml(projectSpec, tempFile);
    ProjectSpec projectSpecCopy = yamlParser.projectSpecFromYaml(tempFile);
    assertEquals(projectSpecCopy, projectSpec);
  }

  /**
   * Tests that parsing a yaml string containing 2 static repetitions and two generators
   * (each generating 10 values), gives a set of parameter sets (with each set unique) of size
   * 200 (2 * 10 * 10).
   *
   * @throws TzarException
   */
  public void testRepetitionDeserialisationAndGeneration() throws TzarException {
    String yaml =
        "repetitions : \n" +
        "  - variables : \n" +
        "      A : 1\n" +
        "  - variables : \n" +
        "      A : 2\n" +
        "generators : \n" +
        "  - key : B \n" +
        "    generator_type : linear_step \n" +
        "    start : 0 \n" +
        "    step_size : 0.1 \n" +
        "    count : 10\n" +
        "  - key : C \n" +
        "    generator_type : normal_distribution \n" +
        "    mean : 0 \n" +
        "    std_dev : 5 \n" +
        "    count : 10";
    Repetitions repetitions = yamlParser.repetitionsFromYaml(yaml);
    assertEquals(LinearStepGenerator.class, repetitions.getGenerators().get(0).getClass());
    assertEquals(NormalDistributionGenerator.class, repetitions.getGenerators().get(1).getClass());
    List<Parameters> paramsList = repetitions.getParamsList();
    HashSet<Parameters> set = Sets.newHashSet(paramsList);
    assertEquals(200, paramsList.size());
    assertEquals(set.size(), paramsList.size()); // assert that all items in the list are unique
  }

  /**
   * Tests that parsing a yaml string containing 2 static repetitions and no generators
   * gives a set of parameter sets (with each set unique) of size 2.
   *
   * @throws TzarException
   */
  public void testRepetitionDeserialisationNoGenerators() throws TzarException {
    String yaml = "{ repetitions : [ { variables : { A : 1 } }, { variables : " +
        "{ A : 2 } } ] }";
    Repetitions repetitions = yamlParser.repetitionsFromYaml(yaml);
    List<Parameters> paramsList = repetitions.getParamsList();
    HashSet<Parameters> set = Sets.newHashSet(paramsList);
    assertEquals(2, paramsList.size());
    assertEquals(set.size(), paramsList.size()); // assert that all items in the list are unique
  }
}
