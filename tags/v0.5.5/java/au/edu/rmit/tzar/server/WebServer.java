package au.edu.rmit.tzar.server;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * A simple webserver based on HttpServer.
 */
public class WebServer implements Runnable {

  private final HttpServer httpServer;

  public WebServer(int port, File baseDir) throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    httpServer.createContext("/", new DirectoryServlet(baseDir));
    httpServer.setExecutor(Executors.newCachedThreadPool());
  }

  public static void main(String[] args) throws Exception {
    new WebServer(Integer.parseInt(args[0]), new File(System.getenv("TZAR_HOME"))).run();
  }

  public void run() {
    httpServer.start();
  }
}
