package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import io.openpixee.java.ChangedFile;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.Weave;
import io.openpixee.java.WeavingResult;
import java.io.File;
import java.io.FileInputStream;
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

  public MavenSecureURLVisitor(File repositoryRootPath, Set<Result> results) {
    this.repositoryRootPath =
        Objects.requireNonNull(repositoryRootPath.toPath().toString(), "repositoryRootPath");
    this.results = results;
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

  private boolean isWithinPhysicalLocation(Set<PhysicalLocation> locations, XMLEvent xmlEvent) {
    var xmlLocation = xmlEvent.getLocation();
    return locations.stream()
        .anyMatch(
            pl ->
                xmlLocation.getLineNumber() >= pl.getRegion().getStartLine()
                    && xmlLocation.getLineNumber() <= pl.getRegion().getEndLine()
                    && xmlLocation.getColumnNumber() >= pl.getRegion().getStartColumn()
                    && xmlLocation.getColumnNumber() <= pl.getRegion().getEndColumn());
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
    // Not even a xml
    if (!file.getPath().endsWith("xml"))
      return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());

    // No suggested changes within file
    var locations = getLocationsWithinFile(file);
    if (locations.isEmpty())
      return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());

    var xmlInputFactory = XMLInputFactory.newInstance();
    var xmlOutputFactory = XMLOutputFactory.newInstance();
    var xmlEventFactory = XMLEventFactory.newInstance();
    try {
      final File modifiedFile =
          File.createTempFile(file.getName(), getExtension(file.getName()).orElse(".tmp"));
      try (FileInputStream fileIS = new FileInputStream(file);
          FileWriter fileWriter = new FileWriter(modifiedFile)) {
        var xmlReader = xmlInputFactory.createXMLEventReader(fileIS);
        var xmlWriter = xmlOutputFactory.createXMLEventWriter(fileWriter);
        while (xmlReader.hasNext()) {
          var nextEvent = xmlReader.nextEvent();
          xmlWriter.add(nextEvent);
          // We search for any url tag and check if it's within any physical location
          if (nextEvent.isStartElement()
              && nextEvent.asStartElement().getName().getLocalPart().equals("url")
              && isWithinPhysicalLocation(locations, nextEvent)) {
            var startElement = nextEvent.asStartElement();
            var url = xmlReader.nextEvent().asCharacters().getData();
            var line = startElement.getLocation().getLineNumber();
            if (url.startsWith("http:")) {
              var fixed = "https:" + url.substring(5);
              xmlWriter.add(xmlEventFactory.createCharacters(fixed));
              var weave = Weave.from(line, ruleId());
              weavingContext.addWeave(weave);
            } else if (url.startsWith("ftp:")) {
              var fixed = "ftps:" + url.substring(4);
              xmlWriter.add(xmlEventFactory.createCharacters(fixed));
              var weave = Weave.from(line, ruleId());
              weavingContext.addWeave(weave);
            } else xmlWriter.add(startElement);
          }
        }
        if (!weavingContext.madeWeaves())
          return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
        var changedFile =
            ChangedFile.createDefault(
                file.getCanonicalPath(), modifiedFile.getAbsolutePath(), weavingContext.weaves());
        return WeavingResult.createDefault(Set.of(changedFile), Collections.emptySet());
      }
    } catch (Exception e) {
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
