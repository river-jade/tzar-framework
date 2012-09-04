package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.SSHClientFactory;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.mockito.Mockito;

import java.io.File;

/**
 * Tests for the SCP file copier.
 */
public class ScpResultsCopierTest extends AbstractResultsCopierTest {

  private SSHClient mockClient;
  private SCPFileTransfer mockFileTransfer;
  private SSHClientFactory mockSSHClientFactory;

  public void setUp() throws Exception {
    super.setUp();
    mockSSHClientFactory = Mockito.mock(SSHClientFactory.class);
    mockClient = Mockito.mock(SSHClient.class);
    mockFileTransfer = Mockito.mock(SCPFileTransfer.class);
  }

  public void testCopyResults() throws Exception {
    Mockito.when(mockSSHClientFactory.createSSHClient()).thenReturn(mockClient);
    Mockito.when(mockClient.newSCPFileTransfer()).thenReturn(mockFileTransfer);
    copier = new ScpResultsCopier(mockSSHClientFactory, baseDestPath);
    copier.copyResults(run, localOutputPath);

    Mockito.verify(mockFileTransfer).upload(
        new FileSystemFile(localOutputPath), baseDestPath.getPath().replace(File.separatorChar, '/'));
  }
}
