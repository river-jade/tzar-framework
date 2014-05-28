package au.edu.rmit.tzar.resultscopier;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;

/**
 * Creates SSHClients using password authentication.
 */
public class SshClientFactoryPasswordAuth extends SshClientFactory {
  private final String sshPassword;

  /**
   * Constructor
   * @param hostname     the hostname of the destination
   * @param sshUserName  username to use to connect to the ssh host
   * @param sshPassword  password to use to connect to the ssh host
   */
  public SshClientFactoryPasswordAuth(String hostname, String sshUserName, String sshPassword) {
    super(hostname, sshUserName);
    this.sshPassword = sshPassword;
  }

  @Override
  protected void authenticate(SSHClient sshClient, String sshUserName) throws UserAuthException, TransportException {
    sshClient.authPassword(sshUserName, sshPassword);
  }
}
