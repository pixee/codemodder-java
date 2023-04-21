package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

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

  @Test
  void it_changes_http_to_https(final @TempDir Path tmpDir) throws IOException {
    String dir = "src/test/resources/maven-non-https-url/";
    copyDir(Path.of(dir), tmpDir);

    List<File> allSarifs = new ArrayList<>();
    Files.newDirectoryStream(tmpDir, "*.sarif")
        .iterator()
        .forEachRemaining(p -> allSarifs.add(p.toFile()));
    Map<String, List<RuleSarif>> map = new SarifParser.Default().parseIntoMap(allSarifs, tmpDir);

    CodemodInvoker codemodInvoker =
        new CodemodInvoker(List.of(MavenSecureURLCodemod.class), tmpDir, map);
    Path pom = tmpDir.resolve("pom_insecure_url_1.xml");
    FileWeavingContext context =
        FileWeavingContext.createDefault(pom.toFile(), IncludesExcludes.any());
    Optional<ChangedFile> changedFileOptional = codemodInvoker.executeFile(pom, context);

    assertThat(
        changedFileOptional.stream()
            .flatMap(cf -> cf.weaves().stream())
            .map(Weave::lineNumber)
            .collect(Collectors.toList()),
        contains(26));
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
