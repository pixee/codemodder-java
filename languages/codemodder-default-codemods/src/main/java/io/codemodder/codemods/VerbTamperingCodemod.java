package io.codemodder.codemods;

import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

/** Removes all {@code <http-method>} XML elements from files named web.xml. */
@Codemod(
    id = "pixee:java/fix-verb-tampering",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class VerbTamperingCodemod implements RawFileChanger {

  private final XPathStreamProcessor processor;

  @Inject
  public VerbTamperingCodemod(final XPathStreamProcessor processor) {
    this.processor = Objects.requireNonNull(processor);
  }

  @Override
  public void visitFile(final CodemodInvocationContext context) throws IOException {
    Path file = context.path();
    if (!file.getFileName().toString().equalsIgnoreCase("web.xml")) {
      return;
    }
    try {
      processWebXml(context, file);
    } catch (SAXException | DocumentException | XMLStreamException e) {
      throw new IOException("Problem transforming web.xml", e);
    }
  }

  private void processWebXml(final CodemodInvocationContext context, final Path file)
      throws SAXException, IOException, DocumentException, XMLStreamException {

    Optional<XPathStreamProcessChange> change =
        processor.process(
            file, "//web-resource-collection/http-method", VerbTamperingCodemod::handle);

    if (change.isEmpty()) {
      return;
    }

    XPathStreamProcessChange xmlChange = change.get();
    Set<Integer> linesAffected = xmlChange.linesAffected();

    // add the weaves to the context
    FileWeavingContext fileWeavingContext = context.changeRecorder();
    linesAffected.stream()
        .map(line -> Weave.from(line, context.codemodId()))
        .forEach(fileWeavingContext::addWeave);

    // overwrite the previous web.xml with the new one
    Files.copy(xmlChange.transformedXml(), file, StandardCopyOption.REPLACE_EXISTING);
  }

  /*
   * Skip the events in the XML that constitute the {@code <http-method>} element.
   */
  private static void handle(
      final XMLEventReader xmlEventReader,
      final XMLEventWriter xmlEventWriter,
      final XMLEvent currentEvent)
      throws XMLStreamException {
    // skip the text event
    XMLEvent httpMethodEvent = xmlEventReader.nextEvent();
    if (!httpMethodEvent.isCharacters()) {
      throw new IllegalStateException("was expecting HTTP method");
    }
    // skip the end element event
    XMLEvent endHttpMethodTag = xmlEventReader.nextEvent();
    if (!endHttpMethodTag.isEndElement()) {
      throw new IllegalStateException("was expecting end element");
    }
  }
}
