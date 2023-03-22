package io.codemodder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public interface XPathStreamEventHandler {
  void handle(XMLEventReader xmlReader, XMLEventWriter xmlWriter, XMLEvent currentEvent)
      throws XMLStreamException;
}
