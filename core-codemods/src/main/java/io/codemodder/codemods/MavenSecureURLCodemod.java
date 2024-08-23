package io.codemodder.codemods;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/** Fixes issues reported under the id "java/maven/non-https-url". */
@Codemod(
    id = "codeql:java/maven/non-https-url",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class MavenSecureURLCodemod extends SarifPluginRawFileChanger
    implements FixOnlyCodeChanger {

  private final XPathStreamProcessor processor;

  @Inject
  MavenSecureURLCodemod(
      @ProvidedCodeQLScan(ruleId = "java/maven/non-https-url") final RuleSarif sarif,
      final XPathStreamProcessor processor) {
    super(sarif);
    this.processor = Objects.requireNonNull(processor);
  }

  @Override
  public String vendorName() {
    return "CodeQL";
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "non-https-url",
        "Failure to use HTTPS or SFTP URL in Maven artifact upload/download",
        "https://codeql.github.com/codeql-query-help/java/java-maven-non-https-url");
  }

  @Override
  public CodemodFileScanningResult onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    try {
      return processXml(context.path(), results);
    } catch (SAXException | DocumentException | IOException | XMLStreamException e) {
      LOG.error("Problem transforming xml file: {}", context.path());
      return CodemodFileScanningResult.none();
    }
  }

  private CodemodFileScanningResult processXml(final Path file, final List<Result> results)
      throws SAXException, IOException, DocumentException, XMLStreamException {
    Optional<XPathStreamProcessChange> change =
        processor.process(
            file,
            "//*[local-name()='repository']/*[local-name()='url'] |"
                + " //*[local-name()='pluginRepository']/*[local-name()='url'] |"
                + " //*[local-name()='snapshotRepository']/*[local-name()='url']",
            MavenSecureURLCodemod::handle);

    if (change.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    XPathStreamProcessChange xmlChange = change.get();
    Set<Integer> linesAffected = xmlChange.linesAffected();

    List<CodemodChange> allWeaves =
        linesAffected.stream()
            .map(
                line -> {
                  Optional<Result> matchingResult =
                      results.stream()
                          .filter(
                              result -> {
                                Region region =
                                    result.getLocations().get(0).getPhysicalLocation().getRegion();
                                Integer resultStartLine = region.getStartLine();
                                Integer resultEndLine = region.getEndLine();
                                return resultStartLine == line
                                    || (resultStartLine <= line
                                        && resultEndLine != null
                                        && resultEndLine >= line);
                              })
                          .findFirst();
                  if (matchingResult.isPresent()) {
                    String id = SarifFindingKeyUtil.buildFindingId(matchingResult.get());
                    Integer sarifLine =
                        matchingResult
                            .get()
                            .getLocations()
                            .get(0)
                            .getPhysicalLocation()
                            .getRegion()
                            .getStartLine();
                    return CodemodChange.from(sarifLine, new FixedFinding(id, detectorRule()));
                  }
                  return CodemodChange.from(line);
                })
            .toList();

    // overwrite the previous web.xml with the new one
    Files.copy(xmlChange.transformedXml(), file, StandardCopyOption.REPLACE_EXISTING);
    return CodemodFileScanningResult.withOnlyChanges(allWeaves);
  }

  /*
   * Change contents of the {@code url} tag if it uses an insecure protocol.
   */
  private static void handle(
      final XMLEventReader xmlEventReader,
      final XMLEventWriter xmlEventWriter,
      final XMLEvent currentEvent)
      throws XMLStreamException {
    final var xmlEventFactory = XMLEventFactory.newInstance();
    xmlEventWriter.add(currentEvent);
    final var nextEvent = xmlEventReader.nextEvent();
    final var url = nextEvent.asCharacters().getData();
    if (url.startsWith("http:")) {
      final var fixed = "https:" + url.substring(5);
      xmlEventWriter.add(xmlEventFactory.createCharacters(fixed));
    } else if (url.startsWith("ftp:")) {
      final var fixed = "ftps:" + url.substring(4);
      xmlEventWriter.add(xmlEventFactory.createCharacters(fixed));
    } else {
      xmlEventWriter.add(nextEvent);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(MavenSecureURLCodemod.class);
}
