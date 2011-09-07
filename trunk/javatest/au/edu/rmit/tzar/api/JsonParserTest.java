package au.edu.rmit.tzar.api;

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
 * Unit tests for the JsonParser class.
 */
public class JsonParserTest extends TestCase {
  JsonParser jsonParser;

  protected void setUp() throws Exception {
    jsonParser = new JsonParser();
  }

  public void testWriteThenReadProjectSpec() throws IOException, RdvException {
    Map<String, Object> variables = Maps.newLinkedHashMap();
    variables.put("test1", 3);
    variables.put("test2", true);
    variables.put("test3", new BigDecimal("3.2")); // we use bigdecimal because floats suffer rounding issues
    Map<String, String> inputFiles = Maps.newLinkedHashMap();
    inputFiles.put("test4", "foo");
    inputFiles.put("test5", "bar");
    Map<String, String> outputFiles = Maps.newLinkedHashMap();

    Parameters baseParameters = new Parameters(variables, inputFiles, outputFiles);

    ArrayList<Scenario> scenarios = Lists.newArrayList();

    variables.put("test1", 7);
    variables.put("test2", false);
    variables.put("test6", 99);
    inputFiles.put("test4", "foo2");
    inputFiles.put("test7", "flurgle");

    Parameters overrideParameters1 = new Parameters(variables, inputFiles, outputFiles);
    Parameters overrideParameters2 = Parameters.EMPTY_PARAMETERS;
    scenarios.add(new Scenario("test scenario1", overrideParameters1));
    scenarios.add(new Scenario("test scenario2", overrideParameters2));
    ProjectSpec projectSpec = new ProjectSpec("test project", baseParameters, scenarios);

    File tempFile = File.createTempFile("json_parser_test", null);
    tempFile.delete();
    jsonParser.projectSpecToJson(projectSpec, tempFile);
    ProjectSpec projectSpecCopy = jsonParser.projectSpecFromJson(tempFile);
    assertEquals(projectSpecCopy, projectSpec);
  }

  /**
   * Tests that parsing a json string containing 2 static repetitions and two generators
   * (each generating 10 values), gives a set of parameter sets (with each set unique) of size
   * 200 (2 * 10 * 10).
   *
   * @throws RdvException
   */
  public void testRepetitionDeserialisationAndGeneration() throws RdvException {
    String json = "{ repetitions : [ { variables : { A : 1 } }, { variables : " +
        "{ A : 2 } } ], generators : [ { key : B, generator_type : linear_step, start : 0, step_size : 0.1, " +
        "count : 10 }, { key : C, generator_type : linear_step, start : 1, step_size : 1, " +
        "count : 10 } ] }";
    Repetitions repetitions = jsonParser.repetitionsFromJson(json);
    List<Parameters> paramsList = repetitions.getParamsList();
    HashSet<Parameters> set = Sets.newHashSet(paramsList);
    assertEquals(200, paramsList.size());
    assertEquals(set.size(), paramsList.size()); // assert that all items in the list are unique
  }
}
