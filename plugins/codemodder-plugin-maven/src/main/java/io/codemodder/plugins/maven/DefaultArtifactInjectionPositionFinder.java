package io.codemodder.plugins.maven;

import com.github.difflib.patch.AbstractDelta;
import java.util.List;

final class DefaultArtifactInjectionPositionFinder implements ArtifactInjectionPositionFinder {

  @Override
  public int find(final List<AbstractDelta<String>> deltas, final String artifactId) {
    String expectedArtifactToken = "<artifactId>" + artifactId + "</artifactId>";
    Integer backupPosition = null;
    for (AbstractDelta<String> delta : deltas) {
      List<String> newLines = delta.getTarget().getLines();
      boolean hasArtifactId =
          newLines.stream().anyMatch(line -> line.contains(expectedArtifactToken));
      boolean hasVersion = newLines.stream().anyMatch(line -> line.contains("<version>"));
      if (hasArtifactId && hasVersion) {
        backupPosition = 1 + delta.getSource().getPosition();
      } else if (hasArtifactId) {
        return 1 + delta.getSource().getPosition();
      }
    }
    if (backupPosition != null) {
      return backupPosition;
    }
    return 1 + deltas.get(0).getSource().getPosition();
  }
}
