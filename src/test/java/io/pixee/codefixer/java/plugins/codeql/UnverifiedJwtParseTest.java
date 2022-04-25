package io.pixee.codefixer.java.plugins.codeql;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

final class UnverifiedJwtParseTest {

  @Test
  void it_prevents_unverified_jwt_parse() throws IOException {
    String insecureFilePath = "src/test/java/com/acme/testcode/JwtVulnerability.java";
    assertJavaWeaveWorkedAndWontReweave(
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
