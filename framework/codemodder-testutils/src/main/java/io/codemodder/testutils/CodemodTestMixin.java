package io.codemodder.testutils;

import static java.nio.file.Files.readAllLines;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import io.codemodder.codetf.*;
import io.codemodder.javaparser.JavaParserFacade;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;

/** The basic tests for codemods. */
public interface CodemodTestMixin {

  @TestFactory
  default Stream<DynamicTest> generateTestCases(@TempDir final Path tmpDir) throws IOException {
    Metadata metadata = getClass().getAnnotation(Metadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException("Test class must be annotated with @Metadata");
    }

    // Test all files with the `.java.before` extension in `testResourceDir`.
    Path testResourceDir = Path.of("src/test/resources/", metadata.testResourceDir());
    Stream<Path> inputStream =
        Files.walk(testResourceDir)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java.before"));

    Function<Path, String> displayNameGenerator =
        p -> p.toString().substring(testResourceDir.toString().length());

    List<DependencyGAV> dependencies =
        Arrays.stream(metadata.dependencies())
            .map(
                dependency -> {
                  String[] tokens = dependency.split(":");
                  return DependencyGAV.createDefault(tokens[0], tokens[1], tokens[2]);
                })
            .toList();

    List<ProjectProvider> projectProviders =
        Arrays.stream(metadata.projectProviders())
            .map(
                projectProvider -> {
                  try {
                    return (ProjectProvider) projectProvider.newInstance();
                  } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();

    ThrowingConsumer<Path> testExecutor =
        path -> {
          // create a new temporary directory for each test case
          final var tmp = Files.createTempDirectory(tmpDir, "test-case-");
          verifyCodemod(
              metadata.codemodType(),
              metadata.renameTestFile(),
              tmp,
              testResourceDir,
              path,
              dependencies,
              projectProviders,
              metadata.doRetransformTest(),
              metadata.expectingFixesAtLines(),
              metadata.expectingFailedFixesAtLines(),
              metadata.sonarJsonFiles());
        };

    final Predicate<String> displayNameFilter =
        metadata.only().isEmpty() ? s -> true : s -> s.matches(metadata.only());
    return DynamicTest.stream(inputStream, displayNameGenerator, testExecutor)
        .filter(test -> displayNameFilter.test(test.getDisplayName()));
  }

  private void verifyCodemod(
      final Class<? extends CodeChanger> codemodType,
      final String renameTestFile,
      final Path tmpDir,
      final Path testResourceDir,
      final Path before,
      final List<DependencyGAV> dependenciesExpected,
      final List<ProjectProvider> projectProviders,
      final boolean doRetransformTest,
      final int[] expectedFixLines,
      final int[] expectingFailedFixesAtLines,
      final String[] sonarJsonFiles)
      throws IOException {

    // create a copy of the test file in the temp directory to serve as our "repository"
    Path after =
        before.resolveSibling(before.getFileName().toString().replace(".before", ".after"));
    Path pathToJavaFile = tmpDir.resolve("Test.java");
    Files.copy(before, pathToJavaFile, StandardCopyOption.REPLACE_EXISTING);

    // rename file if needed
    if (!renameTestFile.isBlank()) {
      Path parent = tmpDir.resolve(renameTestFile).getParent();
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      Path newPathToJavaFile = tmpDir.resolve(renameTestFile);
      Files.copy(pathToJavaFile, newPathToJavaFile, StandardCopyOption.REPLACE_EXISTING);
      pathToJavaFile = newPathToJavaFile;
    }

    final List<Path> sonarJsonsPaths =
        buildSonarJsonPaths(
            testResourceDir,
            sonarJsonFiles,
            List.of("sonar.json", "sonar-issues.json", "sonar-hotspots.json"));

    // Check for any sarif files and build the RuleSarif map
    CodeDirectory codeDir = CodeDirectory.from(tmpDir);
    List<Path> allSarifs = new ArrayList<>();
    Files.newDirectoryStream(testResourceDir, "*.sarif")
        .iterator()
        .forEachRemaining(allSarifs::add);
    Map<String, List<RuleSarif>> map = SarifParser.create().parseIntoMap(allSarifs, codeDir);

    // Check for any a defectdojo
    Path defectDojo = testResourceDir.resolve("defectdojo.json");

    // Check for Contrast
    Path contrastXml = testResourceDir.resolve("contrast.xml");

    // run the codemod
    CodemodLoader loader =
        new CodemodLoader(
            List.of(codemodType),
            CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(pathToJavaFile),
            map,
            List.of(),
            sonarJsonsPaths,
            Files.exists(defectDojo) ? defectDojo : null,
            Files.exists(contrastXml) ? contrastXml : null);

    List<CodemodIdPair> codemods = loader.getCodemods();
    assertThat(codemods.size(), equalTo(1));
    CodemodIdPair codemod = codemods.get(0);
    JavaParserFactory factory = JavaParserFactory.newFactory();
    SourceDirectory dir = SourceDirectory.createDefault(tmpDir, List.of(pathToJavaFile.toString()));
    CodemodExecutor executor =
        CodemodExecutorFactory.from(
            tmpDir,
            IncludesExcludes.any(),
            codemod,
            projectProviders,
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(
                () -> {
                  try {
                    return factory.create(List.of(dir));
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                }),
            EncodingDetector.create());
    CodeTFResult result = executor.execute(List.of(pathToJavaFile));
    List<CodeTFChangesetEntry> changeset = result.getChangeset();

    // let them know if anything failed outright
    assertThat(result.getFailedFiles().size(), equalTo(0));

    // If there is no `.after` file, verify that no changes were made.
    if (Files.notExists(after)) {
      assertThat(result.getChangeset(), is(empty()));
      assertThat(diff(before, pathToJavaFile).getDeltas(), is(empty()));
      return;
    }

    // make sure the file is transformed to the expected output
    verifyTransformedCode(before, after, pathToJavaFile);

    assertThat(changeset.size(), is(1));
    CodeTFChangesetEntry entry = changeset.get(0);
    assertThat(entry.getChanges().isEmpty(), is(false));

    // make sure every change has a line number and description
    for (CodeTFChange change : changeset.get(0).getChanges()) {
      assertThat(change.getLineNumber(), is(greaterThan(0)));
      assertThat(change.getDescription(), is(not(blankOrNullString())));
    }

    ExpectedFixes.verifyExpectedFixes(
        testResourceDir,
        result,
        codemod.getChanger(),
        expectedFixLines,
        expectingFailedFixesAtLines);

    // make sure that some of the basics are being reported
    assertThat(result.getSummary(), is(not(blankOrNullString())));
    assertThat(result.getDescription(), is(not(blankOrNullString())));
    assertThat(result.getReferences(), is(not(empty())));

    // make sure the dependencies are added
    assertThat(dependenciesExpected, hasItems(dependenciesExpected.toArray(new DependencyGAV[0])));

    // tests like those driven by provided SARIF files will not have a retransform test because the
    // concept is nonsensical
    if (!doRetransformTest) {
      return;
    }

    String codeAfterFirstTransform = Files.readString(pathToJavaFile);

    // re-run the transformation again and make sure no changes are made
    CodemodLoader loader2 =
        new CodemodLoader(
            List.of(codemodType),
            CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(pathToJavaFile),
            map,
            List.of(),
            List.of(),
            null,
            null);
    CodemodIdPair codemod2 = loader2.getCodemods().get(0);
    CodemodExecutor executor2 =
        CodemodExecutorFactory.from(
            tmpDir,
            IncludesExcludes.any(),
            codemod2,
            projectProviders,
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(
                () -> {
                  try {
                    return factory.create(List.of(dir));
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }),
            EncodingDetector.create());
    CodeTFResult result2 = executor2.execute(List.of(pathToJavaFile));
    List<CodeTFChangesetEntry> changeset2 = result2.getChangeset();
    assertThat(changeset2, hasSize(0));

    String codeAfterSecondTransform = Files.readString(pathToJavaFile);
    assertThat(codeAfterFirstTransform, equalTo(codeAfterSecondTransform));
  }

  private List<Path> buildSonarJsonPaths(
      final Path testResourceDir,
      final String[] sonarJsonFiles,
      final List<String> defaultSonarFilenames) {
    final List<String> sonarJsons =
        sonarJsonFiles != null ? Arrays.asList(sonarJsonFiles) : new ArrayList<>();

    final List<Path> sonarIssuesJsonsPaths =
        sonarJsons.stream()
            .map(testResourceDir::resolve)
            .filter(Files::exists)
            .collect(Collectors.toList());

    if (sonarIssuesJsonsPaths.isEmpty()) {
      for (String defaultSonarFilename : defaultSonarFilenames) {
        Path defaultPath = testResourceDir.resolve(defaultSonarFilename);
        if (Files.exists(defaultPath)) {
          sonarIssuesJsonsPaths.add(defaultPath);
        }
      }
    }

    return sonarIssuesJsonsPaths;
  }

  /**
   * A hook for verifying the before and after files. By default, this method will compare the
   * contents of the two files for exact equality.
   *
   * @param before a file containing the contents before transformation
   * @param expected the file contents that are expected after transformation
   * @param after a file containing the contents after transformation
   */
  default void verifyTransformedCode(final Path before, final Path expected, final Path after)
      throws IOException {
    Assertions.assertThat(after).hasSameTextualContentAs(expected, StandardCharsets.UTF_8);
  }

  private Patch<String> diff(final Path original, final Path revised) throws IOException {
    return DiffUtils.diff(readAllLines(original), readAllLines(revised));
  }
}
