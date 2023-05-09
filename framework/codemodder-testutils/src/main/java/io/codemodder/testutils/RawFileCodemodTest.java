package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.javaparser.CachingJavaParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Basic tests for {@link RawFileChanger}-based codemods. */
public interface RawFileCodemodTest {

  @Test
  default void it_verifies_codemod(@TempDir final Path tmpDir) throws IOException {
    final Metadata metadata = getClass().getAnnotation(Metadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException("CodemodTest must be annotated with @Metadata");
    }

    final Class<? extends CodeChanger> codemod = metadata.codemodType();
    final Path testResourceDir = Path.of(metadata.testResourceDir());

    final Path testDir = Path.of("src/test/resources/" + testResourceDir);
    verifyCodemod(codemod, tmpDir, testDir);
  }

  /** Verify a single test case composed of a .before and .after file. */
  private void verifySingleCase(
      final CodemodLoader loader,
      final Path tmpDir,
      final Path filePathBefore,
      final Path filePathAfter)
      throws IOException {

    final String tmpFileName = trimExtension(filePathBefore);

    final var tmpFilePath = tmpDir.resolve(tmpFileName);
    Files.copy(filePathBefore, tmpFilePath);

    for (CodemodIdPair pair : loader.getCodemods()) {
      CodemodExecutor executor =
          CodemodExecutor.from(
              tmpDir,
              IncludesExcludes.any(),
              pair,
              List.of(),
              CachingJavaParser.from(new JavaParser()),
              EncodingDetector.create());
      executor.execute(List.of(tmpFilePath));
    }

    final var modifiedFile = Files.readString(tmpFilePath);
    assertThat(modifiedFile, equalTo(Files.readString(filePathAfter)));
  }

  private static String trimExtension(final Path path) {
    return path.getFileName()
        .toString()
        .substring(0, path.getFileName().toString().lastIndexOf('.'));
  }

  private void verifyCodemod(
      final Class<? extends CodeChanger> codemod, final Path tmpDir, final Path testResourceDir)
      throws IOException {
    // find all the sarif files
    final var allSarifFiles =
        Files.list(testResourceDir)
            .filter(file -> file.getFileName().toString().endsWith(".sarif"))
            .collect(Collectors.toList());

    final Map<String, List<RuleSarif>> map =
        SarifParser.create().parseIntoMap(allSarifFiles, tmpDir);

    // run the codemod
    final CodemodLoader invoker = new CodemodLoader(List.of(codemod), tmpDir, map);

    // grab all the .before and .after files in the dir
    final var allBeforeFiles =
        Files.list(testResourceDir)
            .filter(file -> file.getFileName().toString().endsWith(".before"))
            .collect(Collectors.toList());
    final Map<String, Path> afterFilesMap =
        Files.list(testResourceDir)
            .filter(file -> file.getFileName().toString().endsWith(".after"))
            .collect(Collectors.toMap(f -> trimExtension(f), f -> f));

    for (final var beforeFile : allBeforeFiles) {
      final var afterFile = afterFilesMap.get(trimExtension(beforeFile));
      verifySingleCase(invoker, tmpDir, beforeFile, afterFile);
    }
  }
}
