package io.codemodder.codemods;

import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class VerbTamperingCodemod extends RawFileChanger {

  private final XPathStreamProcessor processor;

  @Inject
  public VerbTamperingCodemod(final XPathStreamProcessor processor) {
    this.processor = Objects.requireNonNull(processor);
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
    Path file = context.path();
    if (!"web.xml".equalsIgnoreCase(file.getFileName().toString())) {
      return List.of();
    }
    try {
      return processWebXml(context, file);
    } catch (SAXException | DocumentException | XMLStreamException e) {
      throw new IOException("Problem transforming web.xml", e);
    }
  }

  private List<CodemodChange> processWebXml(final CodemodInvocationContext context, final Path file)
      throws SAXException, IOException, DocumentException, XMLStreamException {
    Optional<XPathStreamProcessChange> change =
        processor.process(
            file, "//web-resource-collection/http-method", VerbTamperingCodemod::handle);

    if (change.isEmpty()) {
      return List.of();
    }

    XPathStreamProcessChange xmlChange = change.get();
    Set<Integer> linesAffected = xmlChange.linesAffected();

    // add the weaves to the context
    List<CodemodChange> changes =
        linesAffected.stream().map(CodemodChange::from).collect(Collectors.toList());

    // overwrite the previous web.xml with the new one
    Files.copy(xmlChange.transformedXml(), file, StandardCopyOption.REPLACE_EXISTING);
    return changes;
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

  @Override
  public String getSummary() {
    return reporter.getSummary();
  }

  @Override
  public String getDescription() {
    return reporter.getDescription();
  }

  @Override
  public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
    return reporter.getChange(filePath, change);
  }

  @Override
  public List<CodeTFReference> getReferences() {
    return reporter.getReferences().stream()
        .map(u -> new CodeTFReference(u, u))
        .collect(Collectors.toList());
  }
}
