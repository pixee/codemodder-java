package io.openpixee.java.plugins.codeql;


import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import io.openpixee.java.protections.WeavingTests;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class UnverifiedJwtParseTest {

  @Test
  void it_prevents_unverified_jwt_parse() throws IOException {
    String insecureFilePath = "src/test/java/com/acme/testcode/JwtVulnerability.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new UnverifiedJwtParseVisitorFactory(
            new File("."),
            Set.of(
                buildResult(insecureFilePath, 13),
                buildResult(insecureFilePath, 19),
                buildResult(insecureFilePath, 27))));
  }

  private Result buildResult(final String insecureFilePath, final int i) {
    return new Result().withLocations(List.of(buildLocation(insecureFilePath, i)));
  }

  private Location buildLocation(final String insecureFilePath, final int line) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(new Region().withStartLine(line))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
