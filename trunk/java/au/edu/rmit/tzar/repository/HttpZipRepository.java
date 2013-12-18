package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A client for a repository which stores files as a zip file accessible
 * via http. This class will retrieve the zip file and extract the contents,
 * returning the path to the extracted contents.
 */
public class HttpZipRepository extends HttpRepository {
  private final HttpRepository httpRepository;

  /**
   * Constructor.
   * @param baseModelsPath the path in which to create the directory containing the
   *                       extracted model files
   * @param sourceUri the URL pointing to the zip file to download
   */
  public HttpZipRepository(File baseModelsPath, URI sourceUri) {
    super(baseModelsPath, sourceUri);
    httpRepository = new HttpRepository(baseModelsPath, sourceUri);
  }

  @Override
  public File retrieveModel(String revision) throws TzarException {
    File tempOutputFile;
    try {
      tempOutputFile = File.createTempFile("httpziprepository", ".zip");
    } catch (IOException e) {
      throw new TzarException("Couldn't create temp file to download zip file into.", e);
    }
    File zipFile = httpRepository.retrieveFile(tempOutputFile);
    try {
      ZipFile zip = new ZipFile(zipFile);
      zip.extractAll(modelPath.getAbsolutePath());
    } catch (ZipException e) {
      throw new TzarException(String.format("Error occurred extracting files from zip file. Perhaps the URL (%s) " +
          "doesn't point to an actual zip file.", sourceUri), e);
    }
    return modelPath;
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision) throws TzarException {
    return httpRepository.retrieveProjectParams(projectParamFilename, revision);
  }

  @Override
  public String getHeadRevision() throws TzarException {
    return httpRepository.getHeadRevision();
  }
}
