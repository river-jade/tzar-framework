package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.api.TzarException;
import com.google.common.io.Closeables;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies results using scp.
 */
public class ScpResultsCopier implements ResultsCopier {
  private static Logger LOG = Logger.getLogger(ScpResultsCopier.class.getName());

  private final File baseDestPath;
  private final SshClientFactory sshClientFactory;

  /**
   * Constructor.
   *
   * @param baseDestPath the base destination path on the remote host
   * @throws IOException if the files cannot be copied
   */
  public ScpResultsCopier(SshClientFactory sshClientFactory, File baseDestPath) throws IOException {
    this.sshClientFactory = sshClientFactory;
    this.baseDestPath = baseDestPath;
  }

  @Override
  public void copyResults(final Run run, final File sourcePath, boolean success) throws TzarException {
    String hostname = sshClientFactory.getHostname();
    LOG.info("Copying results from: " + sourcePath + " to " + hostname + ":" + baseDestPath);
    try {
      boolean thrown = true;
      SSHClient sshClient = null;
      try {
        // we create a new ssh connection for each copy attempt, as we were having issues with
        // connections dropping and not reconnecting. this is a bit less efficient, but more robust.
        sshClient = sshClientFactory.createSSHClient();
        SCPFileTransfer scpFileTransfer = sshClient.newSCPFileTransfer();

        // slight hack here because the SCPFileTransfer class upload method doesn't accept a File object for
        // the destination path (ie only accepts a String), and if this is run on a windows machine where the
        // separator character is '\', a single directory gets created on the destination with name a\b\c,
        // instead of a tree a/b/c.
        // This hack may break if the ssh server is a windows machine.
        scpFileTransfer.upload(new FileSystemFile(sourcePath),
            baseDestPath.getPath().replace(File.separatorChar, '/'));
        run.setRemoteOutputPath(baseDestPath);
        run.setOutputHost(hostname);

        Utils.deleteRecursively(sourcePath.getCanonicalFile());
        LOG.log(Level.INFO, "Copied results for run: {0,number,#} from: {1} to {2}:{3}",
            new Object[]{run.getRunId(), sourcePath, hostname, baseDestPath});
        thrown = false;
      } finally {
        if (sshClient != null) {
          // if an exception was thrown, we make sure we don't
          // throw another one while closing the connection (as this would mean that the first one
          // would be lost).
          Closeables.close(sshClient, thrown);
        }
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }
}
