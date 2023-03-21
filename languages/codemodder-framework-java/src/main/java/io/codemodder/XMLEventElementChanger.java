package io.codemodder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/** TODO: this */
public interface XMLEventElementChanger extends Changer {

  /**
   * Return true if this was modified and we shouldn't call {@link XMLEventWriter#add(XMLEvent)} on
   * the given event.
   */
  boolean process(
      final CodemodInvocationContext invocationContext,
      XMLEventReader xmlReader,
      XMLEventWriter xmlWriter,
      XMLEvent currentEvent)
      throws XMLStreamException;
}
