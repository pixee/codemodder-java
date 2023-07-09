package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfEnvironmentVariable(named = "CODEMODDER_OPENAI_API_KEY", matches = ".+")
final class AddMissingI18nCodemodTest {

  private Path repoRoot;

  @BeforeEach
  void setup(final @TempDir Path tempDir) throws IOException {
    Files.copy(
        Paths.get("src/test/resources/missing-i18n/whatever_bg.properties"),
        tempDir.resolve("whatever_bg.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Paths.get("src/test/resources/missing-i18n/whatever_en_US.properties"),
        tempDir.resolve("whatever_en_US.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Paths.get("src/test/resources/missing-i18n/whatever_es_MX.properties"),
        tempDir.resolve("whatever_es_MX.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Paths.get("src/test/resources/missing-i18n/foo.jsp"),
        tempDir.resolve("foo.jsp"),
        StandardCopyOption.REPLACE_EXISTING);
    repoRoot = tempDir;
  }

  /** Given 3 files, fill in the only missing key in the file. */
  @Test
  void it_fills_in_missing_key() throws IOException {
    // delete a key from the file
    Path englishFile = repoRoot.resolve("whatever_en_US.properties");
    List<String> originalLines = Files.lines(englishFile).collect(Collectors.toList());
    List<String> withoutLoginKey = List.of(originalLines.get(1), originalLines.get(2));
    Files.write(englishFile, withoutLoginKey);

    // now we run the codemod and expect the key to be filled back in
    CodeTFResult result = runCodemod();
    assertThat(
        Files.readString(englishFile),
        CoreMatchers.equalTo(
            Files.readString(
                Paths.get("src/test/resources/missing-i18n/whatever_en_US.properties"))));

    assertThat(result.getFailedFiles().isEmpty(), is(true));
    assertThat(result.getChangeset().size(), is(1));
    CodeTFChangesetEntry entry = result.getChangeset().get(0);
    assertThat(entry.getPath(), CoreMatchers.equalTo(englishFile.getFileName().toString()));
    assertThat(entry.getDiff(), CoreMatchers.equalTo("diff here"));
    List<CodeTFChange> changes = entry.getChanges();
    assertThat(changes.size(), CoreMatchers.equalTo(1));
    CodeTFChange change = changes.get(0);
    assertThat(change.getDescription().contains("Added missing key"), is(true));
    assertThat(change.getDescription().contains("Added missing key"), is(true));
  }

  private CodeTFResult runCodemod() {
    var loader = new CodemodLoader(List.of(AddMissingI18nCodemod.class), repoRoot);
    List<CodemodIdPair> codemods = loader.getCodemods();
    assertThat("Only expecting 1 codemod per test", codemods.size(), equalTo(1));
    CodemodIdPair pair = codemods.get(0);
    CodemodExecutor executor =
        CodemodExecutor.from(
            repoRoot,
            IncludesExcludes.any(),
            pair,
            List.of(),
            List.of(),
            CachingJavaParser.from(new JavaParser()),
            EncodingDetector.create());
    return executor.execute(
        List.of(
            Path.of("whatever_en_US.properties"),
            Path.of("whatever_es_MX.properties"),
            Path.of("whatever_bg.properties")));
  }

  /** Same, but remove usages of the key in the repo, so we don't fill in. */
  @Test
  void it_doesnt_fill_in_when_no_usages() throws IOException {
    Files.delete(repoRoot.resolve("foo.jsp"));
    CodeTFResult result = runCodemod();
    String originalFileContents =
        Files.readString(Paths.get("src/test/resources/missing-i18n/whatever_en_US.properties"));
    assertThat(
        Files.readString(repoRoot.resolve("whatever_en_US.properties")),
        CoreMatchers.equalTo(originalFileContents));
    assertThat(result.getChangeset().isEmpty(), is(true));
  }

  /** Given 3 files, fill in the blank key in the file (instead of being outright missing.) */
  void it_fills_in_blank_key() {}

  /** Same situation, except there is no consistency in the clue values, so we don't fill in. */
  void it_doesnt_fill_in_when_no_idea() {}
}
