package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Region;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RuleSarif;
import io.codemodder.Weave;
import io.codemodder.XMLEventElementChanger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * This changer reads XML results from SARIF, matches them to events that are being visited, and
 * forwards the matching events to subtypes. It is worth noting that this type is limited to
 * forwarding SARIF results that match {@link StartElement} events -- meaning you can only use
 * Semgrep that points to elements, not inner text, attributes, or any other kinds of nodes.
 */
public abstract class SemgrepXMLElementChanger implements XMLEventElementChanger {

  private final RuleSarif sarif;

  public SemgrepXMLElementChanger(final RuleSarif sarif) {
    this.sarif = Objects.requireNonNull(sarif);
  }

  @Override
  public boolean onXmlEventRead(
      final CodemodInvocationContext invocationContext,
      final XMLEventReader xmlReader,
      final XMLEventWriter xmlWriter,
      final XMLEvent currentEvent)
      throws XMLStreamException {
    List<Region> regions = sarif.getRegionsFromResultsByRule(invocationContext.path());
    int lineNumber = currentEvent.getLocation().getLineNumber();
    if (invocationContext.changeRecorder().isLineIncluded(lineNumber)
        && regionMatches(regions, currentEvent)
        && currentEvent.isStartElement()) {
      onSemgrepResultFound(xmlReader, xmlWriter, currentEvent.asStartElement());
      invocationContext
          .changeRecorder()
          .addWeave(Weave.from(lineNumber, invocationContext.codemodId()));
      return true;
    }
    return false;
  }

  /** Return true if the event locations match any of the region locations. */
  private boolean regionMatches(final Collection<Region> regions, final XMLEvent event) {
    Location xmlLocation = event.getLocation();
    return regions.stream()
        .anyMatch(
            region ->
                xmlLocation.getLineNumber() >= region.getStartLine()
                    && xmlLocation.getLineNumber() <= region.getEndLine()
                    && xmlLocation.getColumnNumber() >= region.getStartColumn()
                    && xmlLocation.getColumnNumber() <= region.getEndColumn());
  }

  /**
   * Perform whatever action is needed on this {@link StartElement} that was identified by the
   * Semgrep SARIF.
   *
   * @param xmlReader the reader processing the XML
   * @param xmlWriter the writer which is creating the transformed XML
   * @param element the XML element being visited that matches a SARIF result
   */
  protected abstract void onSemgrepResultFound(
      final XMLEventReader xmlReader, final XMLEventWriter xmlWriter, final StartElement element)
      throws XMLStreamException;
}
