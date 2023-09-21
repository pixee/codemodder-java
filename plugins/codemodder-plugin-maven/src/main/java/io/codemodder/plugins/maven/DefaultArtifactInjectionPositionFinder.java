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
        // if it has the artifact and version, it could be the <dependencyManagement> section, which
        // is 2nd preference
        backupPosition = 1 + delta.getSource().getPosition();
      } else if (hasArtifactId) {
        // if it has the artifact but not the version, it could be the <dependencies> section, which
        // is preferred
        return 1 + delta.getSource().getPosition();
      }
    }
    if (backupPosition != null) {
      return backupPosition;
    }
    // the fallback is to just use the first change to the file
    return 1 + deltas.get(0).getSource().getPosition();
  }
}
