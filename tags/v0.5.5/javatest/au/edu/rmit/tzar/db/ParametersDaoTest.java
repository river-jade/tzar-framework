package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.TzarException;
import com.beust.jcommander.internal.Lists;
import junit.framework.TestCase;

import java.util.List;

/**
 * Unit tests for the ParametersDao class.
 */
public class ParametersDaoTest extends TestCase {
  public void testListToStringAndBack() throws TzarException {
    List<Object> list = Lists.newArrayList();
    list.add("hello there");
    list.add("my name is \"Hortence\", do you like butter?");
    list.add(23);
    list.add(3.14159265);
    String s = ParametersDao.DataType.LIST.toString(list);
    List<Object> otherList = (List<Object>) ParametersDao.DataType.LIST.newInstance(s);
    assertEquals(otherList, list);
  }
}
