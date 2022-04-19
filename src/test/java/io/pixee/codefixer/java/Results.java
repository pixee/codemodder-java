package io.pixee.codefixer.java;

import com.contrastsecurity.sarif.ArtifactContent;
import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import java.util.List;

public final class Results {

  public static Result buildSimpleResult(
      final String path, final int lineNumber, final String snippet, final String ruleId) {
    return new Result()
        .withRuleId(ruleId)
        .withLocations(List.of(buildLocation(path, lineNumber, snippet)));
  }

  private static Location buildLocation(
      final String insecureFilePath, final int line, final String snippet) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(
                    new Region()
                        .withStartLine(line)
                        .withSnippet(new ArtifactContent().withText(snippet)))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
