package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.commands.Utils;

/**
 * Tests for the SCP file copier.
 */
public class ScpResultsCopierTest extends AbstractResultsCopierTest {
  public void setUp() throws Exception {
    super.setUp();
    copier = new ScpResultsCopier("localhost", baseDestDir, Utils.createSSHClient("localhost"));
  }
}
