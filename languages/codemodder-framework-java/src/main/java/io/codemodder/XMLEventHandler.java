package io.codemodder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/** Type that gets used to modify an XML stream. */
public interface XMLEventHandler {

  /** Perform some action when an {@link XMLEvent} is determined to match a SARIF region. */
  void onRegionMatchingXMLEvent(
      XMLEventReader xmlReader, XMLEventWriter xmlWriter, XMLEvent incomingEvent)
      throws XMLStreamException;
}
