package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.commands.CommandFlags;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import java.io.File;

/**
 * Creates SSHClients using pemfile authentication.
 */
public class SshClientFactoryKeyAuth extends SshClientFactory {
  private final File pemFile;

  /**
   * Constructor
   * @param hostname     the hostname of the destination
   * @param sshUserName  username to use to connect to the ssh host
   * @param pemFile      path to the file containing the key for the remote host
   */
  public SshClientFactoryKeyAuth(String hostname, String sshUserName, File pemFile) {
    super(hostname, sshUserName);
    this.pemFile = pemFile;
  }

  public SshClientFactoryKeyAuth(CommandFlags.PollAndRunFlags flags) {
    super(flags.getScpOutputHost(), flags.getScpOutputUser());
    this.pemFile = flags.getPemFile();
  }

  @Override
  protected void authenticate(SSHClient sshClient, String sshUserName) throws UserAuthException, TransportException {
    PKCS8KeyFile keyFile = new PKCS8KeyFile();
    keyFile.init(pemFile);
    sshClient.authPublickey(sshUserName, keyFile);
  }
}
