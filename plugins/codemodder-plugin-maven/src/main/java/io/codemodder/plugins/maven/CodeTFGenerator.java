package io.codemodder.plugins.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.DependencyDescriptor;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.plugins.maven.operator.POMDocument;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class CodeTFGenerator {

  private final ArtifactInjectionPositionFinder positionFinder;

  private final DependencyDescriptor dependencyDescriptor;

  public CodeTFGenerator(
      final ArtifactInjectionPositionFinder positionFinder,
      final DependencyDescriptor dependencyDescriptor) {
    this.dependencyDescriptor = Objects.requireNonNull(dependencyDescriptor);
    this.positionFinder = Objects.requireNonNull(positionFinder);
  }

  public CodeTFChangesetEntry getChanges(
      final Path projectDir, final POMDocument pomDocument, final DependencyGAV newDependency) {
    final List<String> originalPomContents =
        getLinesFrom(pomDocument, pomDocument.getOriginalPom());
    final List<String> finalPomContents =
        getLinesFrom(pomDocument, pomDocument.getResultPomBytes());

    final Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);

    final List<AbstractDelta<String>> deltas = patch.getDeltas();
    final int position = positionFinder.find(deltas, newDependency.artifact());

    final Path pomDocumentPath = getPomDocumentPath(pomDocument);

    final String relativePomPath = projectDir.relativize(pomDocumentPath).toString();

    final String description = dependencyDescriptor.create(newDependency);
    final Map<String, String> properties = buildPropertiesMap(description);
    final CodeTFChange change =
        new CodeTFChange(position, properties, description, List.of(), List.of());

    final List<String> patchDiff =
        UnifiedDiffUtils.generateUnifiedDiff(
            relativePomPath, relativePomPath, originalPomContents, patch, 3);

    final String diff = String.join(pomDocument.getEndl(), patchDiff);

    return new CodeTFChangesetEntry(relativePomPath, diff, List.of(change));
  }

  private Path getPomDocumentPath(final POMDocument pomDocument) {
    try {
      return new File(pomDocument.getPomPath().toURI()).toPath();
    } catch (URISyntaxException e) {
      throw new MavenProvider.DependencyUpdateException(
          "Failure on URI for " + pomDocument.getPomPath(), e);
    }
  }

  private Map<String, String> buildPropertiesMap(final String description) {
    return description != null && !description.isBlank()
        ? Map.of("contextual_description", "true")
        : Collections.emptyMap();
  }

  private List<String> getLinesFrom(final POMDocument doc, final byte[] byteArray) {
    return Arrays.asList(
        new String(byteArray, doc.getCharset()).split(Pattern.quote(doc.getEndl())));
  }
}
