package au.edu.rmit.tzar.repository;

import com.google.common.io.Files;
import junit.framework.Assert;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;

/**
 * Tests for the HttpRepository
 */
public class HttpZipRepositoryTest extends BaseHttpRepositoryTemplate {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    repository = new HttpZipRepository(mockHttpClient, sourceUri, false);
    ByteArrayInputStream bis = new ByteArrayInputStream(BaseHttpRepositoryTemplate.EXPECTED.getBytes());
    ZipParameters zp = new ZipParameters();
    zp.setSourceExternalStream(true);
    File tempFile = File.createTempFile("abc", ".zip");
    tempFile.delete();
    zp.setFileNameInZip(tempFile.getName());
    ZipFile zf = new ZipFile(tempFile);
    zf.addStream(bis, zp);

    returnedByteArray = new ByteArrayEntity(Files.toByteArray(zf.getFile()));
  }

  public void testRetrieveModelSuccess() throws Exception {
    File model = retrieveModel(HttpStatus.SC_OK);

    assertTrue(model.exists());
    assertTrue(model.isDirectory());
    assertEquals(baseModelPath, model.getParentFile());

    File[] files = model.listFiles();
    assertEquals(1, files.length);

    File dataFile = files[0];

    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    String line = reader.readLine();
    Assert.assertEquals(HttpRepositoryTest.EXPECTED, line);
  }
}
