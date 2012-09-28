package au.edu.rmit.tzar;

import au.edu.rmit.tzar.commands.CommandFlags;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spawns new SSHClients.
 */
public class SSHClientFactory {
  private static Logger LOG = Logger.getLogger(SSHClientFactory.class.getName());

  private final String hostname;
  private final File pemFile;
  private final String sshUserName;

  /**
   * Constructor
   * @param hostname     the hostname of the destination
   * @param pemFile      path to the file containing the key for the remote host
   * @param sshUserName  username to use to connect to the ssh host
   */
  public SSHClientFactory(String hostname, File pemFile, String sshUserName) {
    this.hostname = hostname;
    this.sshUserName = sshUserName;
    this.pemFile = pemFile;
  }

  public SSHClientFactory(CommandFlags.PollAndRunFlags flags) {
    this.hostname = flags.getScpOutputHost();
    this.sshUserName = flags.getScpOutputUser();
    this.pemFile = flags.getPemFile();
  }

  public SSHClient createSSHClient() throws IOException {
    File knownHosts = new File(System.getProperty("user.home"), ".ssh/known_hosts");

    final SSHClient sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts));

    LOG.log(Level.INFO, MessageFormat.format("Connecting to SSH host: {0}, with user: {1}, keyfile: {2} and " +
        "known_hosts: {3}", new Object[]{hostname, sshUserName, pemFile, knownHosts}));
    sshClient.useCompression();
    sshClient.connect(hostname);

    PKCS8KeyFile keyFile = new PKCS8KeyFile();
    keyFile.init(pemFile);
    sshClient.authPublickey(sshUserName, keyFile);
    return sshClient;
  }

  public String getHostname() {
    return hostname;
  }
}
