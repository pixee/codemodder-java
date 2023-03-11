package io.openpixee.java.plugins.codeql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.contrastsecurity.sarif.*;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.Weave;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.plugins.JavaSarifMockFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    final var pom = new File("src/test/resources/poms/pom-insecureurl1.xml");

    final var results = Set.of(JavaSarifMockFactory.buildResult(pom.getName(), 22, 8, 27, 22));

    final var weavingResult =
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
        contains(26));
  }

  @Test
  void it_changes_ftp_to_sftp() {
    final var pom = new File("src/test/resources/poms/pom-insecureurl2.xml");

    final var results = Set.of(JavaSarifMockFactory.buildResult(pom.getName(), 22, 8, 27, 22));

    final var weavingResult =
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
        contains(26));
  }

  @Test
  void it_does_not_change_ftp_because_it_is_not_in_the_results() {
    final var pom = new File("src/test/resources/poms/pom-insecureurl2.xml");

    final var results = Set.of(JavaSarifMockFactory.buildResult(pom.getName(), 22, 8, 27, 22));

    final var weavingResult =
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
        contains(26));
  }

  @Test
  void it_does_nothing() {
    final var pom = new File("src/test/resources/poms/pom-insecureurl4.xml");

    final Set<Result> results = Collections.emptySet();

    final var weavingResult =
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
        empty());
  }

  @Test
  void it_preserves_whitespaces() throws IOException {
    final var pom = new File("src/test/resources/poms/pom-insecureurlwonky.xml");

    final var results = Set.of(JavaSarifMockFactory.buildResult(pom.getName(), 26, 8, 37, 22));

    final var weavingResult =
        new MavenSecureURLVisitor(pom.getParentFile(), results)
            .visitRepositoryFile(
                pom.getParentFile(),
                pom,
                FileWeavingContext.createDefault(pom, new IncludesExcludes.MatchesEverything()),
                Collections.emptySet());

    final var changedFile = weavingResult.changedFiles().stream().findAny().get();
    var originalLines = Files.readAllLines(pom.toPath());
    var changedLines = Files.readAllLines(new File(changedFile.modifiedFile()).toPath());
    var deltas = getDeltas(originalLines, changedLines);
    assertThat(deltas.size(), lessThanOrEqualTo(1));

    assertThat(
        weavingResult.changedFiles().stream()
            .flatMap(cf -> cf.weaves().stream())
            .map(Weave::lineNumber)
            .collect(Collectors.toList()),
        contains(34));
  }

  private static List<AbstractDelta<String>> getDeltas(
      final List<String> originalLines, final List<String> comparedLines) {
    final Patch<String> patch = DiffUtils.diff(originalLines, comparedLines);
    return patch.getDeltas();
  }
}
