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

final class InsecureCookieTest {

  @Test
  void it_prevents_insecure_cookie() throws IOException {
    String insecureFilePath = "src/test/java/com/acme/testcode/InsecureCookieVulnerability.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new InsecureCookieVisitorFactory(
            new File("."),
            Set.of(
                buildResult(insecureFilePath, 9),
                buildResult(insecureFilePath, 14),
                buildResult(insecureFilePath, 19),
                buildResult(insecureFilePath, 24),
                buildResult(insecureFilePath, 29),
                buildResult(insecureFilePath, 35))));
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
