package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Copies results using scp.
 */
public class ScpResultsCopier implements ResultsCopier {
  private static Logger LOG = Logger.getLogger(ScpResultsCopier.class.getName());

  private final SCPFileTransfer scpClient;
  private final String hostname;
  private final File baseDestPath;

  /**
   * Constructor.
   *
   * @param hostname     the hostname of the destination
   * @param baseDestPath the base destination path on the remote host
   * @param sshClient    ssh client for file copies
   * @throws IOException if the files cannot be copied
   */
  public ScpResultsCopier(String hostname, File baseDestPath, SSHClient sshClient)
      throws IOException {
    this.hostname = hostname;
    this.baseDestPath = baseDestPath;
    this.scpClient = sshClient.newSCPFileTransfer();
  }

  @Override
  public void copyResults(Run run, File sourcePath) throws RdvException {
    LOG.info("Copying results from: " + sourcePath + " to " + hostname + ":" + baseDestPath);
    try {
      scpClient.upload(sourcePath.getAbsolutePath(), baseDestPath.getPath());
      run.setOutputPath(baseDestPath);
      run.setOutputHost(hostname);
      LOG.info("Copied results from: " + sourcePath + " to " + hostname + ":" + baseDestPath);
    } catch (IOException e) {
      throw new RdvException(e);
    }
  }

  @Override
  public File getBaseDestPath() {
    return baseDestPath;
  }
}