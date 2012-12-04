package au.edu.rmit.tzar.resultscopier;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.UserAuthException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spawns new SSHClients.
 */
public abstract class SshClientFactory {
  private static Logger LOG = Logger.getLogger(SshClientFactory.class.getName());

  private final String hostname;
  private final String sshUserName;

  /**
   * Constructor
   * @param hostname     the hostname of the destination
   * @param sshUserName  username to use to connect to the ssh host
   */
  public SshClientFactory(String hostname, String sshUserName) {
    this.hostname = hostname;
    this.sshUserName = sshUserName;
  }

  public SSHClient createSSHClient() throws IOException {
    File knownHosts = new File(System.getProperty("user.home"), ".ssh/known_hosts");

    final SSHClient sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts));

    LOG.log(Level.INFO, MessageFormat.format("Connecting to SSH host: {0}, with user: {1} and " +
        "known_hosts: {2}", hostname, sshUserName, knownHosts));
    sshClient.useCompression();
    sshClient.connect(hostname);

    authenticate(sshClient, sshUserName);
    return sshClient;
  }

  protected abstract void authenticate(SSHClient sshClient, String sshUserName) throws UserAuthException,
      TransportException;

  public String getHostname() {
    return hostname;
  }
}
