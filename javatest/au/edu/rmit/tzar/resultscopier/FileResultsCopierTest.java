package au.edu.rmit.tzar.resultscopier;

/**
 * Tests that the local filesystem copier copies data as expected.
 */
public class FileResultsCopierTest extends AbstractResultsCopierTest {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    copier = new FileResultsCopier(baseDestDir);
  }
}
