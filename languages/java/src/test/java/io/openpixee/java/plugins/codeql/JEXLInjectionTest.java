package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.protections.WeavingTests;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests relating to JEXL injections detected by CodeQl. See @see <a
 * href=""https://codeql.github.com/codeql-query-help/java/java-jexl-expression-injection/>CodeQL -
 * Expression language injection</a>.
 */
final class JEXLInjectionTest {

  @Disabled
  @Test
  void it_does_nothing() throws IOException {}

  @Test
  void it_sandboxes_jexl_injection() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/jexlinjection/JEXLInjectionSimple.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new JEXLInjectionVisitorFactory(new File("."), Set.of(buildResult(insecureFilePath, 23))),
        new IncludesExcludes.MatchesEverything());
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
