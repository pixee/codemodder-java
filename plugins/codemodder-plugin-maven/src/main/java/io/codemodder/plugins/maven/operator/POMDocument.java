package io.codemodder.plugins.maven.operator;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class POMDocument {

  private byte[] originalPom;
  private URL pomPath;
  private Document pomDocument;

  private Document resultPom;
  private Path path;
  private Charset charset;
  private String endl;
  private String indent;
  private byte[] resultPomBytes;

  /** Preamble Contents are stored here */
  private String preamble;

  /** Afterword - if needed */
  private String suffix;

  private boolean dirty;

  /**
   * Constructs a new `POMDocument` with the specified original POM bytes, path (if available), and
   * DOM contents.
   *
   * @param originalPom The byte array representing the original content of the POM.
   * @param pomPath The URL or file path to the POM.
   * @param pomDocument The DOM (Document Object Model) representation of the POM.
   * @throws URISyntaxException If there is an issue with the provided URL.
   */
  public POMDocument(byte[] originalPom, URL pomPath, Document pomDocument)
      throws URISyntaxException {

    this.originalPom = originalPom;
    this.pomPath = pomPath;
    this.pomDocument = pomDocument;
    this.resultPom = (Document) pomDocument.clone();
    this.path = this.pomPath != null ? Paths.get(this.pomPath.toURI()) : null;
    this.charset = Charset.defaultCharset();
    this.endl = "\n";
    this.indent = "  ";
    this.resultPomBytes = new byte[0];
    this.preamble = "";
    this.suffix = "";
    this.dirty = false;
  }

  /**
   * Constructs a new `POMDocument` with the specified original POM bytes and DOM contents.
   *
   * @param originalPom The byte array representing the original content of the POM.
   * @param pomDocument The DOM (Document Object Model) representation of the POM.
   * @throws URISyntaxException If there is an issue with the provided URL.
   */
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

  public byte[] getOriginalPom() {
    return originalPom;
  }

  public void setOriginalPom(byte[] originalPom) {
    this.originalPom = originalPom;
  }

  public URL getPomPath() {
    return pomPath;
  }

  public void setPomPath(URL pomPath) {
    this.pomPath = pomPath;
  }

  public Document getPomDocument() {
    return pomDocument;
  }

  public void setPomDocument(Document pomDocument) {
    this.pomDocument = pomDocument;
  }

  public Document getResultPom() {
    return resultPom;
  }

  public void setResultPom(Document resultPom) {
    this.resultPom = resultPom;
  }

  public Path getPath() {
    return path;
  }

  public void setPath(Path file) {
    this.path = file;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public String getEndl() {
    return endl;
  }

  public void setEndl(String endl) {
    this.endl = endl;
  }

  public String getIndent() {
    return indent;
  }

  public void setIndent(String indent) {
    this.indent = indent;
  }

  public byte[] getResultPomBytes() {
    return resultPomBytes;
  }

  public void setResultPomBytes(byte[] resultPomBytes) {
    this.resultPomBytes = resultPomBytes;
  }

  public String getPreamble() {
    return preamble;
  }

  public void setPreamble(String preamble) {
    this.preamble = preamble;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }
}
