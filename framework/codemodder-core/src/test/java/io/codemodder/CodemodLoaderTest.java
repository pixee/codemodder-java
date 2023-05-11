package io.codemodder;

import static io.codemodder.CodemodLoader.isValidCodemodId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javaparser.JavaParser;
import io.codemodder.codetf.CodeTFReference;
import io.codemodder.javaparser.CachingJavaParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CodemodLoaderTest {

  @Codemod(
      id = "test_mod",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class InvalidCodemodName extends NoReportChanger {}

  @Codemod(
      id = "test_mod",
      author = " ",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class EmptyCodemodAuthor extends NoReportChanger {}

  @Codemod(
      id = "pixee:java/id",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class ValidCodemod extends NoReportChanger {}

  private static class NoReportChanger implements CodeChanger {
    @Override
    public String getSummary() {
      return "summary";
    }

    @Override
    public String getDescription() {
      return "description";
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return List.of();
    }
  }

  @Test
  void it_validates_codemod_ids() {
    assertThat(isValidCodemodId("pixee:java/id"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("pixee:java/id-with-slashes-numbers-34"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("some-thing:java/id"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:token"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:separator/"), CoreMatchers.is(false));
  }

  @Test
  void it_blows_up_on_duplicate_codemod_ids(@TempDir Path tmpDir) {
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          new CodemodLoader(List.of(ValidCodemod.class, ValidCodemod.class), tmpDir);
        });
  }

  @Codemod(
      id = "test:java/changes-file",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
      author = "test")
  static class ChangesFile extends NoReportChanger implements RawFileChanger {
    @Override
    public List<CodemodChange> visitFile(final CodemodInvocationContext context)
        throws IOException {
      Path path = context.path();
      String contents = Files.readString(path);
      contents += "\nb";
      Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
      return null;
    }
  }

  @Codemod(
      id = "test:java/changes-file-again",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
      author = "test")
  static class ChangesFileAgain extends NoReportChanger implements RawFileChanger {
    @Override
    public List<CodemodChange> visitFile(final CodemodInvocationContext context)
        throws IOException {
      Path path = context.path();
      String contents = Files.readString(path);
      contents += "\nc\n";
      Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
      return null;
    }
  }

  /**
   * We create a file, and then run two codemods on it. The first codemod adds a line, the second
   * adds another. This will ensure that the codemods are run in the order they are provided and
   * that they both had their effect.
   */
  @Test
  void it_handles_two_consecutive_codemod_changes(@TempDir Path tmpDir) throws IOException {
    Path file = tmpDir.resolve("file.txt");
    Files.writeString(file, "a", StandardCharsets.UTF_8);

    List<Class<? extends CodeChanger>> codemods =
        List.of(ChangesFile.class, ChangesFileAgain.class);
    CodemodLoader loader = new CodemodLoader(codemods, tmpDir);

    for (CodemodIdPair codemodIdPair : loader.getCodemods()) {
      CodemodExecutor executor =
          new DefaultCodemodExecutor(
              tmpDir,
              IncludesExcludes.any(),
              codemodIdPair,
              List.of(),
              List.of(),
              CachingJavaParser.from(new JavaParser()),
              EncodingDetector.create());
      executor.execute(List.of(file));
    }

    String contents = Files.readString(file);
    assertThat(contents, equalTo("a\nb\nc\n"));
  }
}
