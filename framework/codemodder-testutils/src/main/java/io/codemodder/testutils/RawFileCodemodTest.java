package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.empty;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserFacade;
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

    MetadataUtil.checkExpectedFixLinesUsage(
        metadata.testResourceDir(),
        metadata.expectingFixesAtLines(),
        metadata.expectingFailedFixesAtLines());

    final Path testDir = Path.of("src/test/resources/" + testResourceDir);
    verifyCodemod(codemod, metadata, tmpDir, testDir);
  }

  /** Verify a single test case composed of a .before and .after file. */
  private void verifySingleCase(
      final Class<? extends CodeChanger> codemod,
      final Path tmpDir,
      final Metadata metadata,
      final Path filePathBefore,
      final Path filePathAfter,
      final Map<String, List<RuleSarif>> ruleSarifMap,
      final int[] expectedFixLines,
      final int[] expectingFailedFixesAtLines)
      throws IOException {

    String tmpFileName = trimExtension(filePathBefore);
    final String renameFile = metadata.renameTestFile();
    if (!renameFile.isBlank()) {
      Path parent = tmpDir.resolve(renameFile).getParent();
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      tmpFileName = renameFile;
    }
    final var tmpFilePath = tmpDir.resolve(tmpFileName);
    Files.copy(filePathBefore, tmpFilePath);

    final CodemodLoader loader =
        new CodemodLoader(
            List.of(codemod),
            CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(tmpFilePath),
            ruleSarifMap,
            List.of(),
            null,
            null,
            null);
    List<CodemodIdPair> codemods = loader.getCodemods();
    assertThat("Only expecting 1 codemod per test", codemods.size(), equalTo(1));

    CodemodIdPair pair = codemods.get(0);
    CodemodExecutor executor =
        CodemodExecutorFactory.from(
            tmpDir,
            IncludesExcludes.any(),
            pair,
            List.of(),
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create());
    CodeTFResult result = executor.execute(List.of(tmpFilePath));

    // let them know if anything failed outright
    assertThat("Some files failed to scan", result.getFailedFiles().size(), equalTo(0));

    // make sure that some of the basics are being reported
    assertThat(result.getSummary(), is(not(blankOrNullString())));
    assertThat(result.getDescription(), is(not(blankOrNullString())));
    assertThat(result.getReferences(), is(not(empty())));

    final var modifiedFile = Files.readString(tmpFilePath);
    if (filePathAfter == null) {
      // lack of an after file indicates that the before file should be unchanged
      assertThat(modifiedFile, equalTo(Files.readString(filePathBefore)));
    } else {
      assertThat(modifiedFile, equalTo(Files.readString(filePathAfter)));
    }
    Files.deleteIfExists(tmpFilePath);

    ExpectedFixes.verifyExpectedFixes(
        result, pair.getChanger(), expectedFixLines, expectingFailedFixesAtLines);
  }

  private static String trimExtension(final Path path) {
    return path.getFileName()
        .toString()
        .substring(0, path.getFileName().toString().lastIndexOf('.'));
  }

  private void verifyCodemod(
      final Class<? extends CodeChanger> codemod,
      final Metadata metadata,
      final Path tmpDir,
      final Path testResourceDir)
      throws IOException {
    // find all the sarif files
    final List<Path> allSarifFiles;
    try (final var files = Files.list(testResourceDir)) {
      allSarifFiles =
          files.filter(file -> file.getFileName().toString().endsWith(".sarif")).toList();
    }

    final Map<String, List<RuleSarif>> map =
        SarifParser.create().parseIntoMap(allSarifFiles, tmpDir);

    // grab all the .before and .after files in the dir
    final List<Path> allBeforeFiles;
    try (final var files = Files.list(testResourceDir)) {
      allBeforeFiles =
          files.filter(file -> file.getFileName().toString().endsWith(".before")).toList();
    }
    final Map<String, Path> afterFilesMap;
    try (final var files = Files.list(testResourceDir)) {
      afterFilesMap =
          files
              .filter(file -> file.getFileName().toString().endsWith(".after"))
              .collect(Collectors.toMap(RawFileCodemodTest::trimExtension, f -> f));
    }
    if (allBeforeFiles.isEmpty()) {
      throw new IllegalArgumentException("No .before files found in " + testResourceDir);
    }

    for (var beforeFile : allBeforeFiles) {
      final var afterFile = afterFilesMap.get(trimExtension(beforeFile));
      // run the codemod
      verifySingleCase(
          codemod,
          tmpDir,
          metadata,
          beforeFile,
          afterFile,
          map,
          metadata.expectingFixesAtLines(),
          metadata.expectingFailedFixesAtLines());
    }
  }
}
