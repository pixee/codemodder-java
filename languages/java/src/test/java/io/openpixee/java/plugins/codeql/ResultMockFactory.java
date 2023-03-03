package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import java.util.List;

public final class ResultMockFactory {

  public static Result buildResult(
      final String insecureFilePath,
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn) {
    return new Result()
        .withLocations(
            List.of(buildLocation(insecureFilePath, beginLine, beginColumn, endLine, endColumn)));
  }

  public static Location buildLocation(
      final String insecureFilePath,
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(
                    new Region()
                        .withStartLine(beginLine)
                        .withStartColumn(beginColumn)
                        .withEndLine(endLine)
                        .withEndColumn(endColumn))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
