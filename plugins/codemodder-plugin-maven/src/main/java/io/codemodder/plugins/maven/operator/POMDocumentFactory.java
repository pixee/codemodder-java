package io.codemodder.plugins.maven.operator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

public class POMDocumentFactory {

  public static POMDocument load(InputStream is)
      throws IOException, DocumentException, URISyntaxException {

    byte[] originalPom = IOUtils.toByteArray(is);

    SAXReader reader = new SAXReader();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(originalPom);
    Document pomDocument = reader.read(inputStream);

    return new POMDocument(originalPom, null, pomDocument);
  }

  public static POMDocument load(File f) throws IOException, DocumentException, URISyntaxException {

    URL fileUrl = f.toURI().toURL();
    return load(fileUrl);
  }

  public static POMDocument load(URL url)
      throws IOException, DocumentException, URISyntaxException {
    InputStream inputStream = url.openStream();
    byte[] originalPom = IOUtils.toByteArray(inputStream);

    SAXReader saxReader = new SAXReader();
    Document pomDocument = saxReader.read(new ByteArrayInputStream(originalPom));

    return new POMDocument(originalPom, url, pomDocument);
  }
}
