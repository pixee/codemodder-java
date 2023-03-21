package io.codemodder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/** A changer for XML files which acts on {@link org.xml.sax.XMLReader} events. */
public interface XMLEventElementChanger extends Changer {

  /**
   * An event that is fired when an {@link XMLEvent} has been read from an XML file.
   *
   * @return true if the state was updated, false if the event should be written to the {@link
   *     XMLEventWriter}
   */
  boolean onXmlEventRead(
      final CodemodInvocationContext invocationContext,
      XMLEventReader xmlReader,
      XMLEventWriter xmlWriter,
      XMLEvent currentEvent)
      throws XMLStreamException;
}
