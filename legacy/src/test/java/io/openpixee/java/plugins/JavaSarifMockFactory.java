package io.openpixee.java.plugins;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import java.util.List;

/** A collection of methods to build mock objects representing SARIF properties and attributes */
public final class JavaSarifMockFactory {

  /** Builds {@link Result} objects based on a path and file coordinates. */
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

  /** Builds {@link Location} objects based on a path and file coordinates. */
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
