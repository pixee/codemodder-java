package io.openpixee.java.plugins.codeql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import com.contrastsecurity.sarif.*;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.Weave;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests relating to insecure protocols usage detected by CodeQl. See @see <a
 * href="https://codeql.github.com/codeql-query-help/java/java-maven-non-https-url/">CodeQL -
 * Failure to use HTTPS or SFTP URL in Maven artifact upload/download </a>.
 */
final class MavenSecureURLTest {

  @Test
  void it_changes_http_to_https() {
    var pom = new File("src/test/resources/poms/pom-insecureurl.xml");

    var results =
        Set.of(
            buildResult(pom.getName(), 31, 8, 36, 22),
            buildResult(pom.getName(), 37, 8, 42, 30),
            buildResult(pom.getName(), 45, 8, 50, 22),
            buildResult(pom.getName(), 53, 8, 58, 28));

    var weavingResult =
        new MavenSecureURLVisitor(pom.getParentFile(), results)
            .visitRepositoryFile(
                pom.getParentFile(),
                pom,
                FileWeavingContext.createDefault(pom, new IncludesExcludes.MatchesEverything()),
                Collections.emptySet());
    assertThat(
        weavingResult.changedFiles().stream()
            .flatMap(cf -> cf.weaves().stream())
            .map(Weave::lineNumber)
            .collect(Collectors.toList()),
        contains(35, 41, 49, 57));
  }

  private static Result buildResult(
      final String insecureFilePath,
      final int startLine,
      final int startColumn,
      final int endLine,
      final int endColumn) {
    return new Result()
        .withLocations(
            List.of(buildLocation(insecureFilePath, startLine, startColumn, endLine, endColumn)));
  }

  private static Location buildLocation(
      final String insecureFilePath,
      final int startLine,
      final int startColumn,
      final int endLine,
      final int endColumn) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(
                    new Region()
                        .withStartLine(startLine)
                        .withEndLine(endLine)
                        .withStartColumn(startColumn)
                        .withEndColumn(endColumn))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
