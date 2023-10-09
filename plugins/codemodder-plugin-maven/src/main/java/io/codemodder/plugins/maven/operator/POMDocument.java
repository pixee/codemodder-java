package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import lombok.Getter;
import lombok.Setter;
import org.dom4j.Document;

/**
 * Data Class to Keep track of an entire POM File, including:
 *
 * <p>Path (pomPath)
 *
 * <p>DOM Contents (pomDocument) - original DOM Contents (resultPom) - modified
 *
 * <p>Charset (ditto) Indent (ditto) Preamble (ditto) Suffix (ditto) Line Endings (endl)
 *
 * <p>Original Content (originalPom) Modified Content (resultPomBytes)
 */
@Getter
@Setter
public class POMDocument {

  private byte[] originalPom;
  private URL pomPath;
  private Document pomDocument;

  private Document resultPom;
  private File file;
  private Charset charset;
  private String endl;
  private String indent;
  private byte[] resultPomBytes;
  private String preamble;
  private String suffix;
  private boolean dirty;

  public POMDocument(byte[] originalPom, URL pomPath, Document pomDocument)
      throws URISyntaxException {

    this.originalPom = originalPom;
    this.pomPath = pomPath;
    this.pomDocument = pomDocument;
    this.resultPom = (Document) pomDocument.clone();
    this.file = this.pomPath != null ? new File(this.pomPath.toURI()) : null;
    this.charset = Charset.defaultCharset();
    this.endl = "\n";
    this.indent = "  ";
    this.resultPomBytes = new byte[0];
    this.preamble = "";
    this.suffix = "";
    this.dirty = false;
  }

  public POMDocument(byte[] originalPom, Document pomDocument) throws URISyntaxException {

    this(originalPom, null, pomDocument);
  }

  @Override
  public String toString() {
    return (pomPath == null) ? "missing" : "[POMDocument @ " + pomPath.toString() + "]";
  }

  public boolean getDirty() {
    return this.dirty;
  }
}
