package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.SAXException;

/** Performs configurable actions on nodes that match XPath expressions. */
public interface XPathStreamProcessor {

  /**
   * Scan the given XML file for nodes that match the given XPath expression and hand them off to
   * the given handler.
   *
   * @return an {@link XPathStreamProcessChange} if the file was changed, otherwise empty
   */
  Optional<XPathStreamProcessChange> process(
      Path path, String xpathExpression, XPathStreamEventHandler handler)
      throws SAXException, IOException, XMLStreamException;
}
