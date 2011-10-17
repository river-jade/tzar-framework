package au.edu.rmit.tzar.commands;

import com.google.common.annotations.VisibleForTesting;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Static Utility functions.
 */
@VisibleForTesting
public class Utils {
  private static Logger LOG = Logger.getLogger(Utils.class.getName());

  public static SSHClient createSSHClient(String hostname)
      throws IOException {
    String username = System.getProperty("user.name");
    File pemFile = new File(System.getProperty("user.home"), ".ssh/id_rsa");
    File knownHosts = new File(System.getProperty("user.home"), ".ssh/known_hosts");

    SSHClient sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts));

    LOG.info("Connecting to SSH host: " + hostname + ", with user: " + username + ", keyfile: " + pemFile +
        " and known_hosts: " + knownHosts);
    sshClient.useCompression();
    sshClient.connect(hostname);

    PKCS8KeyFile keyFile = new PKCS8KeyFile();
    keyFile.init(pemFile);
    sshClient.authPublickey(username, keyFile);
    return sshClient;
  }
}
