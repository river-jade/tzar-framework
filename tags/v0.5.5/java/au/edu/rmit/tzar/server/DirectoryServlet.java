package au.edu.rmit.tzar.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;

/**
 * Basic servlet that recursively serves the contents of a directory.
 */
public class DirectoryServlet implements HttpHandler {
  private final File baseDir;

  /**
   * Constructor.
   * @param baseDir the base directory from which to serve files.
   */
  public DirectoryServlet(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public void handle(HttpExchange t) throws IOException {
    URI uri = t.getRequestURI();
    String path = uri.getPath();
    File file = new File(baseDir, path).getCanonicalFile();
    if (!file.getPath().startsWith(baseDir.getCanonicalPath())) {
      // Possible path traversal attack: reject with 403 error.
      String response = "403 (Forbidden)\n";
      t.sendResponseHeaders(403, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else if (file.isDirectory()) {
      // Object exists and is a file: accept with response code 200.
      t.sendResponseHeaders(200, 0);
      OutputStream os = t.getResponseBody();
      PrintWriter writer = new PrintWriter(os);
      for (File f : file.listFiles()) {
        writer.format("<a href=\"%s\">%s</a><br/>", new File(path, f.getName()).getPath(), f.getName());
      }
      writer.close();
    } else if (!file.isFile()) {
      // Object does not exist or is not a file: reject with 404 error.
      String response = "404 (Not Found)\n";
      t.sendResponseHeaders(404, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else {
      // Object exists and is a file: accept with response code 200.
      t.sendResponseHeaders(200, 0);
      OutputStream os = t.getResponseBody();
      FileInputStream fs = new FileInputStream(file);
      final byte[] buffer = new byte[0x10000];
      int count;
      while ((count = fs.read(buffer)) >= 0) {
        os.write(buffer, 0, count);
      }
      fs.close();
      os.close();
    }
  }
}

