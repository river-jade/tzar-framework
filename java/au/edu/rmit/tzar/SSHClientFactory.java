package au.edu.rmit.tzar;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Spawns new SSHClients.
 */
public class SSHClientFactory {
  private static Logger LOG = Logger.getLogger(SSHClientFactory.class.getName());

  private final String hostname;
  private final File pemFile;

  /**
   * Constructor
   * @param hostname     the hostname of the destination
   * @param pemFile      path to the file containing the key for the remote host, or null to use the default private
   * key on this machine
   */
  public SSHClientFactory(String hostname, File pemFile) {
    this.hostname = hostname;
    if (pemFile == null) {
      pemFile = new File(System.getProperty("user.home"), ".ssh/id_rsa");
    }
    this.pemFile = pemFile;
  }

  public SSHClient createSSHClient() throws IOException {
    String username = System.getProperty("user.name");
    File knownHosts = new File(System.getProperty("user.home"), ".ssh/known_hosts");

    final SSHClient sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts));

    LOG.info("Connecting to SSH host: " + this.hostname + ", with user: " + username + ", keyfile: " + pemFile +
        " and known_hosts: " + knownHosts);
    sshClient.useCompression();
    sshClient.connect(this.hostname);

    PKCS8KeyFile keyFile = new PKCS8KeyFile();
    keyFile.init(pemFile);
    sshClient.authPublickey(username, keyFile);
    return sshClient;
  }

  public String getHostname() {
    return hostname;
  }
}
