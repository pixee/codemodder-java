package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MavenSecureURLCodemodTest {

  private Optional<ChangedFile> invokeAndGetChanges(final String pomFilename, final Path tmpDir)
      throws IOException {
    String dir = "src/test/resources/maven-non-https-url/";
    copyDir(Path.of(dir), tmpDir);

    List<File> allSarifs = new ArrayList<>();
    Files.newDirectoryStream(tmpDir, "*.sarif")
        .iterator()
        .forEachRemaining(p -> allSarifs.add(p.toFile()));
    Map<String, List<RuleSarif>> map = new SarifParser.Default().parseIntoMap(allSarifs, tmpDir);

    CodemodInvoker codemodInvoker =
        new CodemodInvoker(List.of(MavenSecureURLCodemod.class), tmpDir, map);
    Path pom = tmpDir.resolve(pomFilename);
    FileWeavingContext context =
        FileWeavingContext.createDefault(pom.toFile(), IncludesExcludes.any());
    return codemodInvoker.executeFile(pom, context);
  }

  @Test
  void it_changes_http_to_https(final @TempDir Path tmpDir) throws IOException {
    String pomFilename = "pom_insecure_url_1.xml";
    Optional<ChangedFile> changedFileOptional = invokeAndGetChanges(pomFilename, tmpDir);
    assertThat(
        changedFileOptional.stream()
            .flatMap(cf -> cf.weaves().stream())
            .map(Weave::lineNumber)
            .collect(Collectors.toList()),
        contains(26));
  }

  @Test
  void it_changes_ftp_to_sftp(final @TempDir Path tmpDir) throws IOException {
    String pomFilename = "pom_insecure_url_2.xml";
    Optional<ChangedFile> changedFileOptional = invokeAndGetChanges(pomFilename, tmpDir);
    assertThat(
        changedFileOptional.stream()
            .flatMap(cf -> cf.weaves().stream())
            .map(Weave::lineNumber)
            .collect(Collectors.toList()),
        contains(26));
  }

  @Test
  void it_preserves_whitespaces(final @TempDir Path tmpDir) throws IOException {
    String pomFilename = "pom_insecure_url_wonky.xml";
    Optional<ChangedFile> changedFileOptional = invokeAndGetChanges(pomFilename, tmpDir);

    var originalLines = Files.readAllLines(tmpDir.resolve(Path.of(pomFilename)));
    var changedLines =
        Files.readAllLines(new File(changedFileOptional.get().modifiedFile()).toPath());
    var deltas = getDeltas(originalLines, changedLines);

    assertThat(deltas.size(), lessThanOrEqualTo(1));
    assertThat(
        changedFileOptional.stream()
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

  private static void copyDir(Path src, Path dest) throws IOException {
    String srcPath = src.toString();
    String destPath = dest.toString();
    Files.walk(Paths.get(srcPath))
        .forEach(
            a -> {
              Path b = Paths.get(destPath, a.toString().substring(srcPath.length()));
              if (!a.toString().equals(srcPath)) {
                try {
                  Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            });
  }
}
