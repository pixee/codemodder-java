package io.codemodder;

import io.github.pixee.security.XMLInputFactorySecurity;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.QName;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

final class DefaultXPathStreamProcessor implements XPathStreamProcessor {

  @Override
  public Optional<XPathStreamProcessChange> process(
      final Path path, final String xpathExpression, final XPathStreamEventHandler handler)
      throws SAXException, IOException, XMLStreamException {
    SAXReader reader = new PositionCapturingReader();
    reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    reader.setDocumentFactory(new LocatorAwareDocumentFactory());

    String xml = Files.readString(path);
    Document doc;
    try {
      doc = reader.read(new StringReader(xml));
    } catch (DocumentException e) {
      throw new IOException("Problem reading document", e);
    }

    List<Position> httpMethodPositions =
        DocumentHelper.selectNodes(xpathExpression, doc).stream()
            .map(node -> (LocationAwareElement) node)
            .map(element -> new Position(element.getLine(), element.getColumn()))
            .toList();

    if (httpMethodPositions.isEmpty()) {
      return Optional.empty();
    }

    XMLInputFactory inputFactory =
        XMLInputFactorySecurity.hardenFactory(XMLInputFactory.newFactory());
    XMLEventReader xmlReader = inputFactory.createXMLEventReader(Files.newInputStream(path));
    XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    StringWriter sw = new StringWriter();

    XMLEventWriter xmlWriter = outputFactory.createXMLEventWriter(sw);
    while (xmlReader.hasNext()) {
      final XMLEvent currentEvent = xmlReader.nextEvent();
      // get the position of the last character of the event, that is, the start of the next one
      if (xmlReader.hasNext()) {
        Location location = xmlReader.peek().getLocation();
        if (doesPositionMatch(httpMethodPositions, location)) {
          handler.handle(xmlReader, xmlWriter, currentEvent);
        } else {
          xmlWriter.add(currentEvent);
        }
      } else {
        xmlWriter.add(currentEvent);
      }
    }

    String transformedXml = sw.toString();
    if (transformedXml.startsWith("<?xml") && !xml.startsWith("<?xml")) {
      transformedXml = transformedXml.substring(transformedXml.indexOf('>') + 1);
    }

    // Fix prolog string, if needed
    if (xml.stripLeading().startsWith("<?xml")) {
      var xmlHead = xml.substring(0, xml.indexOf('<', xml.indexOf('<') + 1));
      var transformedXmlTail =
          transformedXml.substring(transformedXml.indexOf('<', transformedXml.indexOf('<') + 1));
      transformedXml = xmlHead + transformedXmlTail;
    }

    // remove the empty leftover lines affected by our changes if there are any
    Set<Integer> linesAffected =
        httpMethodPositions.stream().map(pos -> pos.line()).collect(Collectors.toUnmodifiableSet());
    List<String> lines = transformedXml.lines().toList();
    List<String> updatedLines = new ArrayList<>(lines.size() - linesAffected.size());
    for (int i = 1; i <= lines.size(); i++) {
      String actualLine = lines.get(i - 1);
      if (linesAffected.contains(i) && actualLine.isBlank()) {
        continue;
      }
      updatedLines.add(actualLine);
    }

    transformedXml = String.join("\n", updatedLines);

    // if the old file ended with a blank line, make sure to provide one
    if (xml.endsWith("\n") && !transformedXml.endsWith("\n")) {
      transformedXml += "\n";
    }

    Path tmpFile = Files.createTempFile("codemodder", ".xml");
    Files.writeString(tmpFile, transformedXml);
    XPathStreamProcessChange.Default change =
        new XPathStreamProcessChange.Default(linesAffected, tmpFile);
    return Optional.of(change);
  }

  private boolean doesPositionMatch(final List<Position> positions, final Location location) {
    return positions.stream()
        .anyMatch(
            position ->
                position.column() == location.getColumnNumber()
                    && position.line() == location.getLineNumber());
  }

  private static class PositionCapturingReader extends SAXReader {

    @Override
    protected SAXContentHandler createContentHandler(XMLReader reader) {
      return new PositionCapturingContentHandler(getDocumentFactory(), getDispatchHandler());
    }

    @Override
    public void setDocumentFactory(DocumentFactory documentFactory) {
      super.setDocumentFactory(documentFactory);
    }
  }

  private static class PositionCapturingContentHandler extends SAXContentHandler {

    private Locator locator;
    private final DocumentFactory documentFactory;

    public PositionCapturingContentHandler(
        DocumentFactory documentFactory, ElementHandler elementHandler) {
      super(documentFactory, elementHandler);
      this.documentFactory = documentFactory;
    }

    @Override
    public void setDocumentLocator(Locator documentLocator) {
      super.setDocumentLocator(documentLocator);
      this.locator = documentLocator;
      if (documentFactory instanceof LocatorAwareDocumentFactory) {
        ((LocatorAwareDocumentFactory) documentFactory).setLocator(documentLocator);
      }
    }

    public Locator getLocator() {
      return locator;
    }
  }

  private static class LocatorAwareDocumentFactory extends DocumentFactory {

    private Locator locator;

    public LocatorAwareDocumentFactory() {
      super();
    }

    public void setLocator(final Locator locator) {
      this.locator = locator;
    }

    @Override
    public Element createElement(final QName qname) {
      LocationAwareElement element = new LocationAwareElement(qname);
      if (locator != null) {
        element.setLine(locator.getLineNumber());
        element.setColumn(locator.getColumnNumber());
      }
      return element;
    }
  }

  /** An Element that is aware of its location in the source document. */
  private static class LocationAwareElement extends DefaultElement {

    private int lineNumber = -1;
    private int column = -1;

    public LocationAwareElement(final QName qname) {
      super(qname);
    }

    public int getLine() {
      return lineNumber;
    }

    int getColumn() {
      return column;
    }

    public void setLine(int lineNumber) {
      this.lineNumber = lineNumber;
    }

    public void setColumn(int column) {
      this.column = column;
    }
  }
}
