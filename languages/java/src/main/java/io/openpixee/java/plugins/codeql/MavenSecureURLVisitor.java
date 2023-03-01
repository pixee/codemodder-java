package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import io.openpixee.java.ChangedFile;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.Weave;
import io.openpixee.java.WeavingResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fixes issues reported under the id "java/maven-non-https-url". */
public final class MavenSecureURLVisitor implements FileBasedVisitor {

  /** The repository root path. */
  private final String repositoryRootPath;

  /** Set of results for the rule "java/maven-non-https-url" */
  private final Set<Result> results;

  public MavenSecureURLVisitor(final File repositoryRootPath, final Set<Result> results) {
    this.repositoryRootPath =
        Objects.requireNonNull(repositoryRootPath.toPath().toString(), "repositoryRootPath");
    this.results = Objects.requireNonNull(results);
  }

  private Set<PhysicalLocation> getLocationsWithinFile(final File target) {
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
  public String ruleId() {
    return secureURLRuleId;
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final FileWeavingContext weavingContext,
      final Set<ChangedFile> changedJavaFiles) {
    // Not even an xml
    if (!file.getName().toLowerCase().endsWith("xml")) {
      return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
    }

    // No suggested changes within file
    final var locations = getLocationsWithinFile(file);
    if (locations.isEmpty()) {
      return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
    }

    final var xmlInputFactory = XMLInputFactory.newInstance();
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
              final var weave = Weave.from(line, ruleId());
              weavingContext.addWeave(weave);
            } else if (url.startsWith("ftp:")) {
              final var fixed = "ftps:" + url.substring(4);
              xmlWriter.add(xmlEventFactory.createCharacters(fixed));
              final var weave = Weave.from(line, ruleId());
              weavingContext.addWeave(weave);
            } else {
              xmlWriter.add(startElement);
            }
          }
        }
      }
      if (!weavingContext.madeWeaves()) {
        return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
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
          writer.write(reader.read());
        }
      }
      tempFile.delete();

      final var changedFile =
          ChangedFile.createDefault(
              file.getCanonicalPath(), modifiedFile.getAbsolutePath(), weavingContext.weaves());
      return WeavingResult.createDefault(Set.of(changedFile), Collections.emptySet());
    } catch (final Exception e) {
      e.printStackTrace();
      LOG.debug("Problem handling file: {}", file.getPath());
      return WeavingResult.createDefault(Collections.emptySet(), Set.of(file.getAbsolutePath()));
    }
  }

  private static Optional<String> getExtension(final String name) {
    final int dotIndex = name.indexOf('.');
    if (dotIndex != name.length() - 1) {
      return Optional.of(name.substring(dotIndex));
    }
    return Optional.of(name);
  }

  static final String secureURLRuleId = "codeql:java/maven-non-https-url";
  private static final Logger LOG = LoggerFactory.getLogger(MavenSecureURLVisitor.class);
}
