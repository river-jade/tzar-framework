package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A client for a repository which stores files as a zip file accessible
 * via http. This class will retrieve the zip file and extract the contents,
 * returning the path to the extracted contents.
 */
public class HttpZipRepository extends HttpRepository {
  /**
   * Constructor.
   * @param httpClient
   * @param sourceUri the URL pointing to the zip file to download
   * @param skipIfExists don't download the zip file if the expanded library already exists
   */
  public HttpZipRepository(CloseableHttpClient httpClient, URI sourceUri, boolean skipIfExists) {
    super(httpClient, sourceUri, skipIfExists);
  }

  @Override
  void retrieveFile(File outputFile) throws TzarException {
    File zipFile;
    try {
      zipFile = File.createTempFile("httpziprepository", ".zip");
    } catch (IOException e) {
      throw new TzarException("Couldn't create temp file to download zip file into.", e);
    }
    super.retrieveFile(zipFile);
    try {
      ZipFile zip = new ZipFile(zipFile);
      zip.extractAll(outputFile.getAbsolutePath());
    } catch (ZipException e) {
      throw new TzarException(String.format("Error occurred extracting files from zip file. Perhaps the URL (%s) " +
          "doesn't point to an actual zip file.", sourceUri), e);
    }
  }
}
