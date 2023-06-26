package io.codemodder;

import static io.codemodder.CodemodLoader.isValidCodemodId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import io.codemodder.codetf.CodeTFReference;
import io.codemodder.javaparser.CachingJavaParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
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
    public Optional<String> getSourceControlUrl() {
      return Optional.empty();
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return List.of();
    }

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return null;
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
  static class ChangesFile extends RawFileChanger {
    ChangesFile() {
      super(new EmptyReporter());
    }

    public List<CodemodChange> visitFile(final CodemodInvocationContext context)
        throws IOException {
      Path path = context.path();
      String contents = Files.readString(path);
      contents += "\nb";
      Files.writeString(path, contents);
      return List.of();
    }

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

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return null;
    }
  }

  @Codemod(
      id = "test:java/changes-file-again",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
      author = "test")
  static class ChangesFileAgain extends RawFileChanger {

    ChangesFileAgain() {
      super(new EmptyReporter());
    }

    public List<CodemodChange> visitFile(final CodemodInvocationContext context)
        throws IOException {
      Path path = context.path();
      String contents = Files.readString(path);
      contents += "\nc\n";
      Files.writeString(path, contents);
      return List.of();
    }

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

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
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

  @Codemod(
      id = "pixee:java/id",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class ParameterizedCodemod extends NoReportChanger {
    private final Parameter parameter;

    @Inject
    public ParameterizedCodemod(
        @CodemodParameter(
                name = "my-param-number",
                description = "a description of my number parameter",
                defaultValue = "123",
                validationPattern = "\\d+")
            final Parameter parameter) {
      this.parameter = parameter;
    }
  }

  @Test
  void it_uses_default_parameters(@TempDir final Path tmpDir) throws JsonProcessingException {
    List<Class<? extends CodeChanger>> codemods = List.of(ParameterizedCodemod.class);

    // first, we run without any parameters and ensure we get the right stuff
    {
      CodemodLoader loader = new CodemodLoader(codemods, tmpDir);
      CodemodIdPair pair = loader.getCodemods().get(0);
      ParameterizedCodemod changer = (ParameterizedCodemod) pair.getChanger();
      assertThat(changer.parameter.getDefaultValue(), equalTo("123"));
      assertThat(changer.parameter.getValue(null, 5), equalTo("123"));
      assertThat(
          changer.parameter.getDescription(), equalTo("a description of my number parameter"));
    }

    // we need a mapper to read the JSON arguments
    ObjectMapper mapper = new ObjectMapper();

    // next, we run with a parameter without file info and see that it had the intended effects
    {
      String paramJson =
          """
              {
                "codemod": "pixee:java/id",
                "name" : "my-param-number",
                "value" : "456"
              }
              """;
      ParameterArgument param = mapper.readValue(paramJson, ParameterArgument.class);
      CodemodLoader loader = new CodemodLoader(codemods, tmpDir, List.of(param));
      CodemodIdPair pair = loader.getCodemods().get(0);
      ParameterizedCodemod changer = (ParameterizedCodemod) pair.getChanger();
      assertThat(changer.parameter.getDefaultValue(), equalTo("456"));
      assertThat(changer.parameter.getValue(null, 5), equalTo("456"));
      assertThat(
          changer.parameter.getDescription(), equalTo("a description of my number parameter"));
    }

    // next, we run with a parameter with file info and see that it had the intended effects
    Path bar = Path.of("src/main/java/Bar.java");
    Path foo = Path.of("src/main/java/Foo.java");
    {
      String paramJson =
          """
              {
                "codemod": "pixee:java/id",
                "name" : "my-param-number",
                "value" : "456",
                "file" : "src/main/java/Foo.java"
              }
              """;
      ParameterArgument param = mapper.readValue(paramJson, ParameterArgument.class);
      CodemodLoader loader = new CodemodLoader(codemods, tmpDir, List.of(param));
      CodemodIdPair pair = loader.getCodemods().get(0);
      ParameterizedCodemod changer = (ParameterizedCodemod) pair.getChanger();

      // our parameter is file-scoped, so its not the default value
      assertThat(changer.parameter.getDefaultValue(), equalTo("123"));
      // for some random file, we should return the original default value
      assertThat(changer.parameter.getValue(bar, 5), equalTo("123"));
      // for the file we specified, we should return the value we specified
      assertThat(changer.parameter.getValue(foo, 5), equalTo("456"));
      assertThat(
          changer.parameter.getDescription(), equalTo("a description of my number parameter"));
    }

    // finally, we run with a parameter with file and line info and see that it had the intended
    // effects
    {
      String paramJson =
          """
              {
                "codemod": "pixee:java/id",
                "name" : "my-param-number",
                "value" : "456",
                "file" : "src/main/java/Foo.java",
                "line": "5"
              }
              """;
      ParameterArgument param = mapper.readValue(paramJson, ParameterArgument.class);
      CodemodLoader loader = new CodemodLoader(codemods, tmpDir, List.of(param));
      CodemodIdPair pair = loader.getCodemods().get(0);
      ParameterizedCodemod changer = (ParameterizedCodemod) pair.getChanger();

      // our parameter is file-scoped, so its not the default value
      assertThat(changer.parameter.getDefaultValue(), equalTo("123"));
      // for some random file, we should return the original default value
      assertThat(changer.parameter.getValue(bar, 5), equalTo("123"));
      // for the file we specified, we should return the value we specified only on line 5
      assertThat(changer.parameter.getValue(foo, 5), equalTo("456"));
      assertThat(changer.parameter.getValue(foo, 6), equalTo("123"));
      assertThat(
          changer.parameter.getDescription(), equalTo("a description of my number parameter"));
    }
  }
}
