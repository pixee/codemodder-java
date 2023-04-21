package io.codemodder.codemods;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import io.codemodder.*;
import io.codemodder.providers.sarif.codeql.CodeQLScan;
import io.github.pixee.security.BoundedLineReader;
import io.github.pixee.security.XMLInputFactorySecurity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fixes issues reported under the id "java/maven/non-https-url". */
@Codemod(
    id = "codeql:java/maven/non-https-url",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public class MavenSecureURLCodemod extends SarifPluginRawFileChanger {

  @Inject
  MavenSecureURLCodemod(@CodeQLScan(ruleId = "java/maven/non-https-url") final RuleSarif sarif) {
    super(sarif);
  }

  private Set<PhysicalLocation> getLocationsWithinFile(
      final File target, final List<Result> results, final String repositoryRootPath) {
    return results.stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation())
        .filter(
            pl ->
                Path.of(repositoryRootPath, pl.getArtifactLocation().getUri())
                    .equals(target.toPath()))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isWithinPhysicalLocation(
      final Set<PhysicalLocation> locations, final XMLEvent xmlEvent) {
    final var xmlLocation = xmlEvent.getLocation();
    return locations.stream()
        .anyMatch(
            pl ->
                xmlLocation.getLineNumber() >= pl.getRegion().getStartLine()
                    && xmlLocation.getLineNumber() <= pl.getRegion().getEndLine()
                    && xmlLocation.getColumnNumber() >= pl.getRegion().getStartColumn()
                    && xmlLocation.getColumnNumber() <= pl.getRegion().getEndColumn());
  }

  private int secondEventOffset(File file, XMLInputFactory xmlInputFactory) throws Exception {
    try (FileInputStream fileIS = new FileInputStream(file)) {
      final var xmlReader = xmlInputFactory.createXMLEventReader(fileIS);
      xmlReader.nextEvent();
      var second = xmlReader.nextEvent();
      return second.getLocation().getCharacterOffset();
    }
  }

  @Override
  public List<Weave> onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    // Not even an xml
    File file = context.path().toFile();
    // No suggested changes within file
    final var locations =
        getLocationsWithinFile(file, results, context.codeDirectory().asPath().toString());
    if (locations.isEmpty()) {
      return List.of();
    }

    List<Weave> allWeaves = new ArrayList<>();

    final var xmlInputFactory =
        XMLInputFactorySecurity.hardenFactory(XMLInputFactory.newInstance());
    final var xmlOutputFactory = XMLOutputFactory.newInstance();
    final var xmlEventFactory = XMLEventFactory.newInstance();
    try {
      final File tempFile =
          File.createTempFile(file.getName(), getExtension(file.getName()).orElse(".tmp"));
      try (FileInputStream fileIS = new FileInputStream(file);
          FileWriter fileWriter = new FileWriter(tempFile)) {
        final var xmlReader = xmlInputFactory.createXMLEventReader(fileIS);
        final var xmlWriter = xmlOutputFactory.createXMLEventWriter(fileWriter);
        while (xmlReader.hasNext()) {
          final var nextEvent = xmlReader.nextEvent();
          xmlWriter.add(nextEvent);
          // We search for any url tag and check if it's within any physical location
          if (nextEvent.isStartElement()
              && nextEvent.asStartElement().getName().getLocalPart().equals("url")
              && isWithinPhysicalLocation(locations, nextEvent)) {
            final var startElement = nextEvent.asStartElement();
            final var url = xmlReader.nextEvent().asCharacters().getData();
            final var line = startElement.getLocation().getLineNumber();
            if (url.startsWith("http:")) {
              final var fixed = "https:" + url.substring(5);
              xmlWriter.add(xmlEventFactory.createCharacters(fixed));
              final var weave = Weave.from(line, context.codemodId());
              allWeaves.add(weave);
            } else if (url.startsWith("ftp:")) {
              final var fixed = "ftps:" + url.substring(4);
              xmlWriter.add(xmlEventFactory.createCharacters(fixed));
              final var weave = Weave.from(line, context.codemodId());
              allWeaves.add(weave);
            } else {
              xmlWriter.add(startElement);
            }
          }
        }
      }
      if (allWeaves.isEmpty()) {
        return List.of();
      }
      // Workaround for a bug (?) where the whitespaces between prolog and the first tag are ignored
      // Get all the characters until the first 2 events
      // The first one will be the prolog, if it exists
      var originalOffset = secondEventOffset(file, xmlInputFactory);
      var tempOffset = secondEventOffset(tempFile, xmlInputFactory);

      // We want to glue the "head" of the original file with the tail of the tempFile
      var originalHead = "";
      try (FileInputStream fileIS = new FileInputStream(file)) {
        originalHead = new String(fileIS.readNBytes(originalOffset));
      }

      final File modifiedFile =
          File.createTempFile(file.getName(), getExtension(file.getName()).orElse(".tmp"));
      try (BufferedReader reader = new BufferedReader(new FileReader(tempFile));
          BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedFile))) {
        reader.skip(tempOffset);
        writer.append(originalHead);
        while (reader.ready()) {
          writer.write(BoundedLineReader.readLine(reader, 1000000));
        }
      }
      tempFile.deleteOnExit();

      return allWeaves;
    } catch (final Exception e) {
      LOG.error("Problem handling file: {}", file.getPath(), e);
      return List.of();
    }
  }

  private static Optional<String> getExtension(final String name) {
    final int dotIndex = name.indexOf('.');
    if (dotIndex != name.length() - 1) {
      return Optional.of(name.substring(dotIndex));
    }
    return Optional.of(name);
  }

  private static final Logger LOG = LoggerFactory.getLogger(MavenSecureURLCodemod.class);
}
