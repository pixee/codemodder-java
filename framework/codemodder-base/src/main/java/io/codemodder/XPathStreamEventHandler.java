package io.codemodder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/** A handler for {@link XMLEvent}s that match an XPath expression. */
public interface XPathStreamEventHandler {

  /** Handle an {@link XMLEvent} that matched an XPath expression. */
  void handle(XMLEventReader xmlReader, XMLEventWriter xmlWriter, XMLEvent currentEvent)
      throws XMLStreamException;
}
