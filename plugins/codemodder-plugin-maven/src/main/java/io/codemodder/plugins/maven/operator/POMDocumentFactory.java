package io.codemodder.plugins.maven.operator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * Factory class for creating instances of the {@link POMDocument} class, which represents a POM
 * (Project Object Model) file along with its various properties and contents.
 */
class POMDocumentFactory {

  /**
   * Loads a POM document from the provided input stream.
   *
   * @param is The input stream containing the POM document data.
   * @return A new instance of {@link POMDocument} representing the loaded POM.
   * @throws IOException If an I/O error occurs while reading the input stream.
   * @throws DocumentException If an error occurs while parsing the POM document.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   */
  public static POMDocument load(InputStream is)
      throws IOException, DocumentException, URISyntaxException {

    byte[] originalPom = IOUtils.toByteArray(is);

    SAXReader reader = new SAXReader();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(originalPom);
    Document pomDocument = reader.read(inputStream);

    return new POMDocument(originalPom, null, pomDocument);
  }

  /**
   * Loads a POM document from the provided file.
   *
   * @param filePath The file representing the POM document.
   * @return A new instance of {@link POMDocument} representing the loaded POM.
   * @throws IOException If an I/O error occurs while reading the file.
   * @throws DocumentException If an error occurs while parsing the POM document.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   */
  public static POMDocument load(Path filePath)
      throws IOException, DocumentException, URISyntaxException {
    URL fileUrl = filePath.toUri().toURL();
    return load(fileUrl);
  }

  /**
   * Loads a POM document from the provided URL.
   *
   * @param url The URL of the POM document.
   * @return A new instance of {@link POMDocument} representing the loaded POM.
   * @throws IOException If an I/O error occurs while reading the URL.
   * @throws DocumentException If an error occurs while parsing the POM document.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   */
  public static POMDocument load(URL url)
      throws IOException, DocumentException, URISyntaxException {
    InputStream inputStream = url.openStream();
    byte[] originalPom = IOUtils.toByteArray(inputStream);

    SAXReader saxReader = new SAXReader();
    Document pomDocument = saxReader.read(new ByteArrayInputStream(originalPom));

    return new POMDocument(originalPom, url, pomDocument);
  }
}
