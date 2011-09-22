package au.edu.rmit.tzar.parser;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.RdvException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Tests for the Repetitions class.
 */
public class RepetitionsTest extends TestCase {

  private static final String KEY = "key";
  private List<Parameters> staticRepetitions;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    staticRepetitions = new ArrayList<Parameters>();
    staticRepetitions.add(Parameters.createParameters(
        new HashMap<String, Object>() {{
          put("x", "y");
        }},
        new HashMap<String, String>() {{
          put("a", "1.txt");
        }},
        new HashMap<String, String>() {{
          put("b", "2.txt");
        }}
    ));
    staticRepetitions.add(Parameters.createParameters(
        new HashMap<String, Object>() {{
          put("x", "z");
        }},
        new HashMap<String, String>() {{
          put("a", "3.txt");
        }},
        new HashMap<String, String>() {{
          put("b", "4.txt");
        }}
    ));

  }

  /**
   * Tests that if there are no generated repetitions define, the parameters list is
   * just equal to the static repetitions.
   */
  public void testStaticRepetitions() throws RdvException {
    Repetitions repetitions = new Repetitions(staticRepetitions, null);
    assertEquals(staticRepetitions, repetitions.getParamsList());
  }

  /**
   * Tests basic linear step generation with no static repetitions.
   */
  public void testGeneratedRepetitions() throws RdvException {
    List<RepetitionGenerator<?>> generators = Lists.newArrayList();
    generators.add(new LinearStepGenerator(KEY, BigDecimal.valueOf(1.0), 10, BigDecimal.valueOf(2.0)));
    Repetitions repetitions = new Repetitions(null, generators);
    List<Parameters> paramsList = repetitions.getParamsList();
    assertEquals(10, paramsList.size());
    for (int i = 0; i < 10; i++) {
      Parameters parameters = paramsList.get(i);
      ImmutableMap<String, Object> variables = parameters.getVariables();
      assertEquals(1, variables.size());
      assertEquals(0, parameters.getInputFiles().size());
      assertEquals(0, parameters.getOutputFiles().size());
      assertEquals(BigDecimal.valueOf(1.0 + (i * 2)), variables.get(KEY));
    }
  }
}
