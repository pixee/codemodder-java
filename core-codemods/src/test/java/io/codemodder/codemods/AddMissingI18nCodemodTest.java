package io.codemodder.codemods;

import static io.codemodder.codemods.AddMissingI18nCodemod.getPropertyFilePrefix;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserFacade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.ListLanguagesRequest;
import software.amazon.awssdk.services.translate.model.ListLanguagesResponse;

@EnabledIf("awsAvailable")
final class AddMissingI18nCodemodTest {

  private Path repoRoot;

  /**
   * We setup the "repo" to have 3 matching properties files with all the same uniform keys. Our
   * tests will mess with this clean and good setup in order to achieve our test goals.
   */
  @BeforeEach
  void setup(final @TempDir Path tempDir) throws IOException {
    Files.copy(
        Path.of("src/test/resources/missing-i18n/whatever_bg.properties"),
        tempDir.resolve("whatever_bg.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Path.of("src/test/resources/missing-i18n/whatever_en_US.properties"),
        tempDir.resolve("whatever_en_US.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Path.of("src/test/resources/missing-i18n/whatever_es_MX.properties"),
        tempDir.resolve("whatever_es_MX.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.copy(
        Path.of("src/test/resources/missing-i18n/foo.jsp"),
        tempDir.resolve("foo.jsp"),
        StandardCopyOption.REPLACE_EXISTING);
    repoRoot = tempDir;
  }

  /** Given 3 files, fill in the only missing key in the en_US properties. */
  @Test
  void it_fills_in_missing_key() throws IOException {
    // delete a key from the en_US properties file
    Path targetEnglishFile = repoRoot.resolve("whatever_en_US.properties");
    List<String> originalLines = Files.lines(targetEnglishFile).toList();
    List<String> withoutLoginKey = List.of(originalLines.get(1), originalLines.get(2));
    Files.write(targetEnglishFile, withoutLoginKey);

    // now we run the codemod and expect the key to be added back in at the end
    CodeTFResult result = runCodemod();
    assertThat(
        Files.readString(targetEnglishFile),
        CoreMatchers.equalTo(
            """
                        choose.character=Click on your favorite character to start the game!
                        choose.character.title=Choose your avatar
                        login.button.name=Access
                        """));

    assertThat(result.getFailedFiles().isEmpty(), is(true));
    assertThat(result.getChangeset().size(), is(1));
    CodeTFChangesetEntry entry = result.getChangeset().get(0);
    assertThat(entry.getPath(), CoreMatchers.equalTo(targetEnglishFile.getFileName().toString()));
    assertThat(
        entry.getDiff(),
        CoreMatchers.equalTo(
            """
            --- whatever_en_US.properties
            +++ whatever_en_US.properties
            @@ -1,2 +1,3 @@
             choose.character=Click on your favorite character to start the game!
             choose.character.title=Choose your avatar
            +login.button.name=Access"""));
    List<CodeTFChange> changes = entry.getChanges();
    assertThat(changes.size(), CoreMatchers.equalTo(1));
    CodeTFChange change = changes.get(0);
    assertThat(
        change
            .getDescription()
            .contains(
                """
            Added missing i18n key value for "login.button.name". The new value was based on other other property files that had values for the key.
            This key was confirmed to be in use in  1 place(s), including:"""),
        is(true));
    assertThat(
        change.getDescription().contains("foo.jsp: <button label=\"login.button.name\"></button>"),
        is(true));
  }

  /** Same, but remove usage of the key in the repo, so we don't fill in. */
  @Test
  void it_doesnt_fill_in_when_no_usages() throws IOException {
    // delete a key from the file
    Path targetEnglishFile = repoRoot.resolve("whatever_en_US.properties");
    List<String> originalLines = Files.lines(targetEnglishFile).toList();
    List<String> withoutLoginKey = List.of(originalLines.get(1), originalLines.get(2));
    Files.write(targetEnglishFile, withoutLoginKey);

    // delete the usage of the given key
    Files.delete(repoRoot.resolve("foo.jsp"));

    // now we run the codemod and expect no changes and the file to have remained the same (still
    // missing the key)
    CodeTFResult result = runCodemod();
    assertThat(
        Files.readString(repoRoot.resolve("whatever_en_US.properties")),
        CoreMatchers.equalTo(
            """
                        choose.character=Click on your favorite character to start the game!
                        choose.character.title=Choose your avatar
                        """));
    assertThat(result.getChangeset().isEmpty(), is(true));
  }

  @Test
  void it_gracefully_errors_on_unknown_languages() throws IOException {
    // create a nonsense language code contents
    Path zzlangFile = repoRoot.resolve("whatever_zz.properties");
    String zzzlangFileContents =
        """
            login.button.name=Zzfiji3r1z
            choose.character=Zzfiji3r1z Zzfiji3r1z Zzfiji3r1z ZzfijZzfiji3r1z Zzfiji3r1z Zzfiji3r1z i3r1z
            choose.character.title=Zzfiji3r1z Zzfiji3r1z Zzfiji3r1z
            """;

    // fill it with nonsense
    Files.writeString(zzlangFile, zzzlangFileContents);

    // run the codemods, hopefully not erroring
    CodeTFResult result = runCodemod();

    // the file contents should not have changed
    assertThat(Files.readString(zzlangFile), CoreMatchers.equalTo(zzzlangFileContents));

    // there should be no changes reported
    assertThat(result.getChangeset().isEmpty(), is(true));
    assertThat(result.getFailedFiles().isEmpty(), is(true));
  }

  @Test
  void it_matches_all_property_files() {
    assertThat(getPropertyFilePrefix("foo.txt"), is(Optional.empty()));
    assertThat(getPropertyFilePrefix("foo.properties"), is(Optional.empty()));
    assertThat(getPropertyFilePrefix("foo_en.properties"), equalTo(Optional.of("foo")));
    assertThat(getPropertyFilePrefix("foo_en_US.properties"), equalTo(Optional.of("foo")));
    assertThat(getPropertyFilePrefix("foo_es_MX.properties"), equalTo(Optional.of("foo")));
  }

  /** Given 3 files, fill in the blank key in the file (instead of being outright missing.) */
  @Test
  void it_fills_in_blank_key() throws IOException {
    // delete a key from the en_US properties file
    Path targetEnglishFile = repoRoot.resolve("whatever_en_US.properties");
    Files.writeString(
        targetEnglishFile,
        """
                    login.button.name=
                    choose.character=Click on your favorite character to start the game!
                    choose.character.title=Choose your avatar
                    """);

    // now we run the codemod and expect the key to be filled in
    CodeTFResult result = runCodemod();
    assertThat(
        Files.readString(targetEnglishFile),
        CoreMatchers.equalTo(
            """
                            login.button.name=Access
                            choose.character=Click on your favorite character to start the game!
                            choose.character.title=Choose your avatar
                            """));

    assertThat(result.getFailedFiles().isEmpty(), is(true));
    assertThat(result.getChangeset().size(), is(1));
    CodeTFChangesetEntry entry = result.getChangeset().get(0);
    assertThat(entry.getPath(), CoreMatchers.equalTo(targetEnglishFile.getFileName().toString()));
    assertThat(
        entry.getDiff(),
        CoreMatchers.equalTo(
            """
            --- whatever_en_US.properties
            +++ whatever_en_US.properties
            @@ -1,3 +1,3 @@
            -login.button.name=
            +login.button.name=Access
             choose.character=Click on your favorite character to start the game!
             choose.character.title=Choose your avatar"""));
    List<CodeTFChange> changes = entry.getChanges();
    assertThat(changes.size(), CoreMatchers.equalTo(1));
    CodeTFChange change = changes.get(0);
    assertThat(
        change
            .getDescription()
            .contains(
                """
            Added missing i18n key value for "login.button.name". The new value was based on other other property files that had values for the key.
            This key was confirmed to be in use in  1 place(s), including:"""),
        is(true));
    assertThat(
        change.getDescription().contains("foo.jsp: <button label=\"login.button.name\"></button>"),
        is(true));
  }

  private CodeTFResult runCodemod() throws IOException {
    CodemodLoader loader = createLoader(AddMissingI18nCodemod.class, repoRoot);
    List<CodemodIdPair> codemods = loader.getCodemods();
    assertThat("Only expecting 1 codemod per test", codemods.size(), equalTo(1));
    CodemodIdPair pair = codemods.get(0);
    CodemodExecutor executor =
        CodemodExecutorFactory.from(
            repoRoot,
            IncludesExcludes.any(),
            pair,
            List.of(),
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create());
    return executor.execute(
        List.of(
            repoRoot.resolve("whatever_en_US.properties"),
            repoRoot.resolve("whatever_es_MX.properties"),
            repoRoot.resolve("whatever_bg.properties")));
  }

  private CodemodLoader createLoader(final Class<? extends CodeChanger> codemodType, final Path dir)
      throws IOException {
    return new CodemodLoader(
        List.of(codemodType),
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repoRoot,
        List.of("**"),
        List.of(),
        Files.list(dir).toList(),
        Map.of(),
        List.of(),
        null);
  }

  /** If we can't connect to AWS, skip the test. */
  private static boolean awsAvailable() {
    try {
      TranslateClient client = TranslateClient.builder().build();
      ListLanguagesResponse response = client.listLanguages(ListLanguagesRequest.builder().build());
      return !response.languages().isEmpty();
    } catch (Exception e) {
      System.out.println("Skipping test because AWS is not available");
    }
    return false;
  }
}
