package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.codemodder.Changer;
import io.codemodder.CodemodInvoker;
import io.codemodder.FileWeavingContext;
import io.codemodder.IncludesExcludes;
import io.codemodder.RuleSarif;
import io.codemodder.SarifParser;
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

    final Class<? extends Changer> codemod = metadata.codemodType();
    final Path testResourceDir = Path.of(metadata.testResourceDir());

    final Path testDir = Path.of("src/test/resources/" + testResourceDir);
    verifyCodemod(codemod, tmpDir, testDir);
  }

  /** Verify a single test case composed of a .before and .after file. */
  private void verifySingleCase(
      final CodemodInvoker codemodInvoker,
      final Path tmpDir,
      final Path filePathBefore,
      final Path filePathAfter)
      throws IOException {

    final String tmpFileName = trimExtension(filePathBefore);

    final var tmpFilePath = tmpDir.resolve(tmpFileName);
    Files.copy(filePathBefore, tmpFilePath);
    final FileWeavingContext context =
        FileWeavingContext.createDefault(tmpFilePath.toFile(), IncludesExcludes.any());
    final var maybeModifiedFilePath =
        codemodInvoker.executeFile(tmpFilePath, context).map(cf -> cf.modifiedFile()).map(Path::of);
    if (maybeModifiedFilePath.isEmpty()) {
      throw new IllegalArgumentException("Problem transforming file: " + tmpFileName);
    }
    final var modifiedFile = Files.readString(maybeModifiedFilePath.get());
    assertThat(modifiedFile, equalTo(Files.readString(filePathAfter)));
  }

  private static String trimExtension(final Path path) {
    return path.getFileName()
        .toString()
        .substring(0, path.getFileName().toString().lastIndexOf('.'));
  }

  private void verifyCodemod(
      final Class<? extends Changer> codemod, final Path tmpDir, final Path testResourceDir)
      throws IOException {
    // find all the sarif files
    final var allSarifFiles =
        Files.list(testResourceDir)
            .filter(file -> file.getFileName().toString().endsWith(".sarif"))
            .map(path -> path.toFile())
            .collect(Collectors.toList());

    final Map<String, List<RuleSarif>> map =
        new SarifParser.Default().parseIntoMap(allSarifFiles, tmpDir);

    // run the codemod
    final CodemodInvoker invoker = new CodemodInvoker(List.of(codemod), tmpDir, map);

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
