package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.beans.DownloadMode;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import au.edu.rmit.tzar.runners.mapreduce.Concatenator;
import au.edu.rmit.tzar.runners.mapreduce.FileSelector;
import au.edu.rmit.tzar.runners.mapreduce.MapReduce;
import au.edu.rmit.tzar.runners.mapreduce.Reducer;
import com.google.common.collect.*;
import junit.framework.TestCase;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the YamlParser class.
 */
public class YamlParserTest extends TestCase {
  private static final String RUNNER_CLASS = "TheRunnerClass";
  private static final String RUNNER_FLAGS = "-flag1 -flag2";
  YamlParser yamlParser;
  private CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
  private CodeSourceFactory mockCodeSourceFactory = mock(CodeSourceFactory.class);

  protected void setUp() throws Exception {
    yamlParser = new YamlParser();
  }

  public void testWriteThenReadProjectSpec() throws Exception {
    Map<String, CodeSourceImpl> libraries = Maps.newLinkedHashMap();
    CodeSourceImpl.RepositoryTypeImpl localFile = CodeSourceImpl.RepositoryTypeImpl.LOCAL_FILE;
    CodeSourceImpl codeSource1 = new CodeSourceImpl(mockHttpClient, new URI("/source/code/1"),
        localFile, "123", DownloadMode.FORCE);
    libraries.put("library1", codeSource1);
    CodeSourceImpl codeSource2 = new CodeSourceImpl(mockHttpClient, new URI("/source/code/2"),
        localFile, "124", DownloadMode.FORCE);
    libraries.put("library2", codeSource2);
    CodeSourceImpl codeSource3 = new CodeSourceImpl(mockHttpClient, new URI("/source/code/3"),
        localFile, "125", DownloadMode.CACHE);
    libraries.put("library3", codeSource3);

    when(mockCodeSourceFactory.createCodeSource("123", localFile, Utils.makeAbsoluteUri("/source/code/1"),
        DownloadMode.FORCE)).thenReturn(codeSource1);
    when(mockCodeSourceFactory.createCodeSource("124", localFile, Utils.makeAbsoluteUri("/source/code/2"),
        DownloadMode.FORCE)).thenReturn(codeSource2);
    when(mockCodeSourceFactory.createCodeSource("125", localFile, Utils.makeAbsoluteUri("/source/code/3"),
        DownloadMode.CACHE)).thenReturn(codeSource3);

    Map<String, Object> variables = Maps.newLinkedHashMap();
    variables.put("test1", 3);
    variables.put("test2", true);
    variables.put("test3", new BigDecimal("3.2")); // we use bigdecimal because floats suffer rounding issues
    Map<String, String> inputFiles = Maps.newLinkedHashMap();
    inputFiles.put("test4", "foo");
    inputFiles.put("test5", "bar");

    Parameters baseParameters = Parameters.createParameters(variables);

    ArrayList<Scenario> scenarios = Lists.newArrayList();

    variables.put("test1", 7);
    variables.put("test2", false);
    variables.put("test6", 99);
    inputFiles.put("test4", "foo2");
    inputFiles.put("test7", "flurgle");

    Parameters overrideParameters1 = Parameters.createParameters(variables);
    Parameters overrideParameters2 = Parameters.EMPTY_PARAMETERS;
    scenarios.add(new Scenario("test scenario1", overrideParameters1));
    scenarios.add(new Scenario("test scenario2", overrideParameters2));

    List<Parameters> staticRepetitions = ImmutableList.of(overrideParameters1);
    List<RepetitionGenerator<?>> generators = ImmutableList.<RepetitionGenerator<?>>of(
        new LinearStepGenerator("test8", BigDecimal.valueOf(23), 2, BigDecimal.valueOf(2)),
        new NormalDistributionGenerator("test9", BigDecimal.TEN, 3, BigDecimal.ONE));
    Repetitions repetitions = new Repetitions(staticRepetitions, generators);
    FileSelector mapper = new FileSelector();
    mapper.setFlags(ImmutableMap.of("filenames", "value1", "flag2", "value2"));
    Reducer reducer = new Concatenator();
    reducer.setFlags(ImmutableMap.of("flag3", "value3", "flag4", "value4"));
    MapReduce mapReduce = new MapReduce(mapper, reducer);
    ProjectSpecImpl projectSpec = new ProjectSpecImpl("test project", RUNNER_CLASS, RUNNER_FLAGS, baseParameters,
        scenarios, repetitions, libraries, mapReduce);

    File tempFile = File.createTempFile("yaml_parser_test", null);
    tempFile.delete();
    yamlParser.projectSpecToYaml(projectSpec, tempFile);
    ProjectSpec projectSpecCopy = yamlParser.projectSpecFromYaml(tempFile, mockCodeSourceFactory);
    assertEquals(projectSpec, projectSpecCopy);
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
        "static_repetitions : \n" +
            "  - A : 1\n" +
            "  - A : 2\n" +
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
    assertEquals(1, repetitions.getStaticRepetitions().get(0).asMap().get("A"));
    assertEquals(2, repetitions.getStaticRepetitions().get(1).asMap().get("A"));

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
    String yaml = "{ static_repetitions : [ { variables : { A : 1 } }, { variables : " +
        "{ A : 2 } } ] }";
    Repetitions repetitions = yamlParser.repetitionsFromYaml(yaml);
    List<Parameters> paramsList = repetitions.getParamsList();
    HashSet<Parameters> set = Sets.newHashSet(paramsList);
    assertEquals(2, paramsList.size());
    assertEquals(set.size(), paramsList.size()); // assert that all items in the list are unique
  }
}
