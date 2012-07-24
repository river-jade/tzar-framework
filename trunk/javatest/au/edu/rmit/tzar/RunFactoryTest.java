package au.edu.rmit.tzar;

import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.parser.Repetitions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for RunFactory.
 */
public class RunFactoryTest extends TestCase {
  private static final String REVISION = "rev1";
  private static final String COMMAND_FLAGS = "--some_flag";
  private static final String RUNSET = "a_runset";
  private ProjectSpec mockProjectSpec;
  private Repetitions mockRepetitions;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockProjectSpec = mock(ProjectSpec.class);
    mockRepetitions = mock(Repetitions.class);
  }

  /**
   * Test that given repetitions, and a project spec with scenarios, that the correct number of runs
   * is created, and that the parameters are applied in correct priority order: ie project, scenario, then
   * repetitions (from lowest priority to highest).
   */
  public void testCreateRuns() throws RdvException {
    Parameters globalParams = Parameters.createParameters(ImmutableMap.of("A", -1, "B", -1, "C", -1, "D", -1),
        ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of());

    RunFactory runFactory = new RunFactory(REVISION, COMMAND_FLAGS, RUNSET, mockProjectSpec, mockRepetitions,
        globalParams);

    when(mockProjectSpec.getBaseParams()).thenReturn(
        Parameters.createParameters(ImmutableMap.of("A", 0, "B", 0, "C", 0),
            ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of()));


    List<Scenario> scenarios = ImmutableList.of(
        new Scenario("scenario1", Parameters.createParameters(ImmutableMap.of("A", 1, "B", 1, "C", 1),
        ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of())),
        new Scenario("scenario1", Parameters.createParameters(ImmutableMap.of("A", 2, "B", 2, "C", 2),
            ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of())));
    when(mockProjectSpec.getScenarios()).thenReturn(scenarios);

    List<Parameters> repetitionsParams = new ArrayList<Parameters>();
    repetitionsParams.add(Parameters.createParameters(ImmutableMap.of("A", 3, "B", 4),
        ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of()));
    repetitionsParams.add(Parameters.createParameters(ImmutableMap.of("A", 5, "B", 6),
        ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of()));
    when(mockRepetitions.getParamsList()).thenReturn(repetitionsParams);

    List<Run> runs = runFactory.createRuns(2);
    assertEquals(8, runs.size()); // 2 scenarios, 2 repetitions,numRuns = 2 => 2*2*2 = 8 runs

    @SuppressWarnings("unchecked")
    List<ImmutableList<Integer>> expected = Lists.newArrayList(
        ImmutableList.of(3, 4, 1, -1),
        ImmutableList.of(5, 6, 1, -1),
        ImmutableList.of(3, 4, 2, -1),
        ImmutableList.of(5, 6, 2, -1),
        ImmutableList.of(3, 4, 1, -1),
        ImmutableList.of(5, 6, 1, -1),
        ImmutableList.of(3, 4, 2, -1),
        ImmutableList.of(5, 6, 2, -1)
    );

    for (Run run : runs) {
      ImmutableMap<String, Object> variables = run.getParameters().getVariables();
      assertTrue(expected.remove(ImmutableList.of(
          (Integer)variables.get("A"),
          (Integer)variables.get("B"),
          (Integer)variables.get("C"),
          (Integer)variables.get("D")
      )));
    }
    assertEquals(0, expected.size());
  }
}
