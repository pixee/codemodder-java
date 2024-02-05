package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.codetf.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.javaparser.JavaParserFacade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link DefaultCodemodExecutor} that ensure CodeTF is generated as expected. */
final class DefaultCodemodExecutorTest {

  private Path repoDir;
  private DefaultCodemodExecutor executor;
  private Path javaFile1;
  private Path javaFile2;
  private Path javaFile3;
  private Path javaFile4;
  private EncodingDetector encodingDetector;
  private JavaParserFacade javaParserFacade;
  private IncludesExcludes includesEverything;
  private BeforeToAfterChanger beforeToAfterChanger;
  private CodemodIdPair beforeAfterCodemod;
  private FileCache fileCache;

  @BeforeEach
  void setup(final @TempDir Path tmpDir) throws IOException {
    this.repoDir = tmpDir;
    beforeToAfterChanger = new BeforeToAfterChanger();
    beforeAfterCodemod = new CodemodIdPair("codemodder:java/id", beforeToAfterChanger);
    javaParserFacade = JavaParserFacade.from(JavaParser::new);
    encodingDetector = EncodingDetector.create();
    includesEverything = IncludesExcludes.any();
    fileCache = FileCache.createDefault();
    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            beforeAfterCodemod,
            List.of(),
            List.of(),
            fileCache,
            javaParserFacade,
            encodingDetector,
            -1,
            -1,
            -1);

    javaFile1 = repoDir.resolve("Test1.java");
    Files.write(javaFile1, List.of("class Test1 {", "  void before() {}", "}"));
    javaFile2 = repoDir.resolve("Test2.java");
    Files.write(javaFile2, List.of("class Test2 {", "  void needsDep1() {}", "}"));
    javaFile3 = repoDir.resolve("Test3.java");
    Files.write(
        javaFile3, List.of("class Test3 {", "  void nothing() {}", "  void before() {}", "}"));
    javaFile4 = repoDir.resolve("Test4.java");
    Files.write(javaFile4, List.of("class Test4 {", "  void needsDep2() {}", "}"));

    // our fake dependency file
    Path depsFile = repoDir.resolve("deps.txt");
    Files.write(depsFile, List.of("my-org:my-existing-dependency:1.0.0"));
  }

  /**
   * This type provides dependency injection features for an imaginary dependency mgmt tool called
   * "deps". It simply add the dependencies, one per line, to the bottom of a dep.txt file at the
   * root of the project.
   */
  static class FakeDepsProvider implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        final Path projectDir, final Path file, final List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      Path deps = projectDir.resolve("deps.txt");
      List<String> oldDeps = Files.readAllLines(deps);

      List<DependencyGAV> skippedDependencies =
          remainingFileDependencies.stream()
              .filter(d -> oldDeps.contains(DefaultCodemodExecutor.toPackageUrl(d)))
              .collect(Collectors.toList());

      List<String> newDeps =
          remainingFileDependencies.stream()
              .filter(d -> !skippedDependencies.contains(d))
              .map(DefaultCodemodExecutor::toPackageUrl)
              .collect(Collectors.toList());

      // if no new dependencies to add, nope out
      if (newDeps.isEmpty()) {
        return DependencyUpdateResult.create(List.of(), skippedDependencies, List.of(), Set.of());
      }

      List<String> allDeps = new ArrayList<>();
      allDeps.addAll(oldDeps);
      allDeps.addAll(newDeps);
      String allDepsText = String.join("\n", allDeps);
      Files.writeString(deps, allDepsText);

      List<String> patchDiff =
          UnifiedDiffUtils.generateUnifiedDiff(
              deps.getFileName().toString(),
              deps.getFileName().toString(),
              oldDeps,
              DiffUtils.diff(oldDeps, allDeps),
              3);
      String diff = String.join("\n", patchDiff);

      CodeTFPackageAction packageAddResult =
          new CodeTFPackageAction(
              CodeTFPackageAction.CodeTFPackageActionType.ADD,
              CodeTFPackageAction.CodeTFPackageActionResult.COMPLETED,
              String.join(",", newDeps));
      CodeTFChange change =
          new CodeTFChange(
              oldDeps.size() + 1,
              Collections.emptyMap(),
              "updated deps",
              CodeTFDiffSide.LEFT,
              List.of(packageAddResult),
              List.of());
      CodeTFChangesetEntry entry = new CodeTFChangesetEntry("deps.txt", diff, List.of(change));
      List<CodeTFChangesetEntry> changes = List.of(entry);
      return DependencyUpdateResult.create(
          remainingFileDependencies, skippedDependencies, changes, Set.of());
    }
  }

  @Test
  void it_generates_single_codemod_codetf() {
    CodeTFResult result = executor.execute(List.of(javaFile1));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBeforeAfterCodemodMetadata);

    // should have just 1 entry because we only scanned javaFile1
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(1);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile1ChangedCorrectly);
  }

  @Test
  void it_allows_codetf_customization() {

    CodeTFProvider addsPrefixProvider =
        new CodeTFProvider() {
          @Override
          public CodeTFResult onResultCreated(final CodeTFResult result) {
            return CodeTFResult.basedOn(result)
                .withSummary("hi, " + result.getSummary())
                .withDescription("hi, " + result.getDescription())
                .withAdditionalReferences(List.of(new CodeTFReference("https://hi/", "hi")))
                .build();
          }

          @Override
          public CodeTFChange onChangeCreated(
              final Path path, final String codemod, final CodeTFChange change) {
            return CodeTFChange.basedOn(change)
                .withDescription("hi " + path.toString())
                .withAdditionalProperties(Map.of("hi", codemod))
                .build();
          }
        };

    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            beforeAfterCodemod,
            List.of(),
            List.of(addsPrefixProvider),
            fileCache,
            javaParserFacade,
            encodingDetector,
            -1,
            -1,
            -1);

    CodeTFResult result = executor.execute(List.of(javaFile1));

    // confirm the result was updated by the provider
    assertThat(result.getSummary()).isEqualTo("hi, before-after-summary");
    assertThat(result.getDescription()).isEqualTo("hi, before-after-description");
    assertThat(result.getReferences()).hasSize(2);
    assertThat(result.getReferences().get(1).getDescription()).isEqualTo("hi");
    assertThat(result.getReferences().get(1).getUrl()).isEqualTo("https://hi/");

    // confirm the change was updated by the provider
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(1);
    CodeTFChangesetEntry entry = changeset.get(0);
    assertThat(entry.getChanges().get(0).getDescription()).isEqualTo("hi " + javaFile1.toString());
    assertThat(entry.getChanges().get(0).getProperties()).hasSize(1);
    assertThat(entry.getChanges().get(0).getProperties())
        .hasFieldOrPropertyWithValue("hi", beforeAfterCodemod.getId());
  }

  @Test
  void it_generates_all_files_codemod_codetf() {
    CodeTFResult result = executor.execute(List.of(javaFile1, javaFile2, javaFile3));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBeforeAfterCodemodMetadata);

    // should have 2 entries for both javaFile1 and javaFile3
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(2);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile1ChangedCorrectly);
    assertThat(changeset.get(1)).satisfies(DefaultCodemodExecutorTest::isJavaFile3ChangedCorrectly);
  }

  @Test
  void it_respects_max_files() {
    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            beforeAfterCodemod,
            List.of(),
            List.of(),
            fileCache,
            javaParserFacade,
            encodingDetector,
            -1,
            1,
            -1);

    CodeTFResult result = executor.execute(List.of(javaFile1, javaFile2, javaFile3));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBeforeAfterCodemodMetadata);

    // should have only 1 entry for javaFile1 because we only allow scanning 1 file
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(1);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile1ChangedCorrectly);
  }

  @Test
  void it_respects_max_file_size() throws IOException {
    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            beforeAfterCodemod,
            List.of(),
            List.of(),
            fileCache,
            javaParserFacade,
            encodingDetector,
            500,
            -1,
            -1);

    // make javaFile1 too big to scan
    String javaFile1Contents = Files.readString(javaFile1);
    javaFile1Contents = " ".repeat(1000) + javaFile1Contents;
    Files.writeString(javaFile1, javaFile1Contents);

    // scan like normal
    CodeTFResult result = executor.execute(List.of(javaFile1, javaFile2, javaFile3));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBeforeAfterCodemodMetadata);

    // should have only 1 entry for javaFile3 because javaFile1 is too big
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(1);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile3ChangedCorrectly);
  }

  @Test
  void it_reports_failed_files() throws IOException {
    // make the java1 file not parseable so we can confirm errors reported correctly
    Files.writeString(javaFile1, "this is not java code");
    CodeTFResult result = executor.execute(List.of(javaFile1, javaFile2, javaFile3));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBeforeAfterCodemodMetadata);

    // should have 2 entries for both javaFile1 and javaFile3
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(result.getFailedFiles()).hasSize(1);
    assertThat(result.getFailedFiles()).contains("Test1.java");
    assertThat(changeset.size()).isEqualTo(1);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile3ChangedCorrectly);
  }

  @Test
  void it_handles_cumulative_file_and_dependency_changes() throws IOException {
    List<CodemodIdPair> codemods = new ArrayList<>();
    codemods.add(new CodemodIdPair("codemodder:java/inject-dep-1", new InjectsDependency1()));
    codemods.add(new CodemodIdPair("codemodder:java/inject-dep-2", new InjectsDependency2()));
    FakeDepsProvider depsProvider = new FakeDepsProvider();
    List<CodeTFResult> results = new ArrayList<>();
    for (CodemodIdPair codemod : codemods) {
      executor =
          new DefaultCodemodExecutor(
              repoDir,
              includesEverything,
              codemod,
              List.of(depsProvider),
              List.of(),
              fileCache,
              JavaParserFacade.from(JavaParser::new),
              EncodingDetector.create(),
              -1,
              -1,
              -1);
      CodeTFResult result = executor.execute(List.of(javaFile2, javaFile4));
      results.add(result);
    }
    CodeTFReport report =
        CodeTFReportGenerator.createDefault()
            .createReport(repoDir, "cmd line args here", List.of(), results, 100);
    assertThat(report).isNotNull();
    assertThat(report.getResults()).hasSize(2);

    CodeTFResult injectDependency1Result = report.getResults().get(0);
    List<CodeTFChangesetEntry> firstChangeset = injectDependency1Result.getChangeset();
    assertThat(firstChangeset).hasSize(2);
    assertThat(injectDependency1Result.getCodemod()).isEqualTo("codemodder:java/inject-dep-1");
    assertThat(injectDependency1Result.getFailedFiles()).isEmpty();
    assertThat(injectDependency1Result.getSummary()).isEqualTo("injects-dependency-1-summary");
    assertThat(injectDependency1Result.getDescription())
        .isEqualTo("injects-dependency-1-description");
    assertThat(injectDependency1Result.getReferences())
        .isEqualTo(List.of(new CodeTFReference("https://dep1.com/", "https://dep1.com/")));
    CodeTFChangesetEntry javaFile2Entry = firstChangeset.get(0);
    assertThat(javaFile2Entry.getPath()).isEqualTo("Test2.java");
    assertThat(javaFile2Entry.getChanges()).hasSize(1);
    CodeTFChange firstCodeChange = javaFile2Entry.getChanges().get(0);
    assertThat(firstCodeChange.getLineNumber()).isEqualTo(2);
    assertThat(firstCodeChange.getDescription()).isEqualTo("injects-dependency-1-change");
    List<CodeTFPackageAction> javaFile2PackageActions = firstCodeChange.getPackageActions();
    assertThat(javaFile2PackageActions.size()).isEqualTo(1);
    assertThat(javaFile2PackageActions.get(0).getAction())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionType.ADD);
    assertThat(javaFile2PackageActions.get(0).getPackage())
        .isEqualTo("pkg:maven/org.spring/dep1@1.1.1");
    assertThat(javaFile2PackageActions.get(0).getResult())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionResult.COMPLETED);
    assertThat(javaFile2Entry.getDiff())
        .isEqualTo(
            "--- Test2.java\n"
                + "+++ Test2.java\n"
                + "@@ -1,3 +1,5 @@\n"
                + " class Test2 {\n"
                + "-  void needsDep1() {}\n"
                + "+  void needsDep1() {\n"
                + "+      Dependency1.doStuff();\n"
                + "+  }\n"
                + " }");
    CodeTFChangesetEntry firstDepsEntry = firstChangeset.get(1);
    assertThat(firstDepsEntry.getPath()).isEqualTo("deps.txt");
    assertThat(firstDepsEntry.getDiff())
        .isEqualTo(
            "--- deps.txt\n"
                + "+++ deps.txt\n"
                + "@@ -1,1 +1,2 @@\n"
                + " my-org:my-existing-dependency:1.0.0\n"
                + "+pkg:maven/org.spring/dep1@1.1.1");

    CodeTFResult injectDependency2Result = report.getResults().get(1);
    List<CodeTFChangesetEntry> secondChangeset = injectDependency2Result.getChangeset();
    assertThat(secondChangeset).hasSize(2);
    assertThat(injectDependency2Result.getCodemod()).isEqualTo("codemodder:java/inject-dep-2");
    assertThat(injectDependency2Result.getFailedFiles()).isEmpty();
    assertThat(injectDependency2Result.getSummary()).isEqualTo("injects-dependency-2-summary");
    assertThat(injectDependency2Result.getReferences())
        .isEqualTo(List.of(new CodeTFReference("https://dep2.com/", "https://dep2.com/")));
    assertThat(injectDependency2Result.getDescription())
        .isEqualTo("injects-dependency-2-description");
    CodeTFChangesetEntry javaFile4Entry = secondChangeset.get(0);
    assertThat(javaFile4Entry.getPath()).isEqualTo("Test4.java");
    assertThat(javaFile4Entry.getChanges()).hasSize(1);
    assertThat(javaFile4Entry.getDiff())
        .isEqualTo(
            "--- Test4.java\n"
                + "+++ Test4.java\n"
                + "@@ -1,3 +1,5 @@\n"
                + " class Test4 {\n"
                + "-  void needsDep2() {}\n"
                + "+  void needsDep2() {\n"
                + "+      Dependency2.doStuff();\n"
                + "+  }\n"
                + " }");

    CodeTFChange secondCodeChange = javaFile4Entry.getChanges().get(0);
    List<CodeTFPackageAction> javaFile4PackageActions = secondCodeChange.getPackageActions();
    assertThat(javaFile4PackageActions.size()).isEqualTo(1);
    assertThat(javaFile4PackageActions.get(0).getAction())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionType.ADD);
    assertThat(javaFile4PackageActions.get(0).getPackage())
        .isEqualTo("pkg:maven/org.apache/dep2@2.2.2");
    assertThat(javaFile4PackageActions.get(0).getResult())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionResult.COMPLETED);

    CodeTFChangesetEntry secondDepsEntry = secondChangeset.get(1);
    assertThat(secondDepsEntry.getPath()).isEqualTo("deps.txt");
    assertThat(secondDepsEntry.getDiff())
        .isEqualTo(
            "--- deps.txt\n"
                + "+++ deps.txt\n"
                + "@@ -1,2 +1,3 @@\n"
                + " my-org:my-existing-dependency:1.0.0\n"
                + " pkg:maven/org.spring/dep1@1.1.1\n"
                + "+pkg:maven/org.apache/dep2@2.2.2");

    // assert that the report can be serialized by jackson to json without error
    ObjectWriter writer =
        new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .writerWithDefaultPrettyPrinter();
    String codetf = writer.writeValueAsString(report);
    assertThat(codetf).isNotBlank();
  }

  @Test
  void it_reports_dependency_updating_failure() {
    CodemodIdPair codemod =
        new CodemodIdPair("codemodder:java/inject-dep-1", new InjectsDependency1());
    ProjectProvider badProvider =
        (projectDir, file, remainingFileDependencies) -> DependencyUpdateResult.EMPTY_UPDATE;

    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            codemod,
            List.of(badProvider),
            List.of(),
            fileCache,
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create(),
            -1,
            -1,
            -1);
    CodeTFResult result = executor.execute(List.of(javaFile2, javaFile4));
    List<CodeTFChangesetEntry> firstChangeset = result.getChangeset();

    // should have changed the java file, but not the deps.txt file
    assertThat(firstChangeset).hasSize(1);
    assertThat(result.getCodemod()).isEqualTo("codemodder:java/inject-dep-1");
    assertThat(result.getFailedFiles()).isEmpty();
    CodeTFChangesetEntry javaFile2Entry = firstChangeset.get(0);
    assertThat(javaFile2Entry.getPath()).isEqualTo("Test2.java");
    assertThat(javaFile2Entry.getChanges()).hasSize(1);
    List<CodeTFPackageAction> packageActions =
        javaFile2Entry.getChanges().get(0).getPackageActions();

    assertThat(packageActions.size()).isEqualTo(1);
    CodeTFPackageAction packageAction = packageActions.get(0);
    assertThat(packageAction.getAction())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionType.ADD);
    assertThat(packageAction.getPackage()).isEqualTo("pkg:maven/org.spring/dep1@1.1.1");
    assertThat(packageAction.getResult())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionResult.FAILED);
    assertThat(javaFile2Entry.getDiff())
        .isEqualTo(
            "--- Test2.java\n"
                + "+++ Test2.java\n"
                + "@@ -1,3 +1,5 @@\n"
                + " class Test2 {\n"
                + "-  void needsDep1() {}\n"
                + "+  void needsDep1() {\n"
                + "+      Dependency1.doStuff();\n"
                + "+  }\n"
                + " }");
  }

  @Test
  void it_reports_dependency_updating_skipping() throws IOException {
    CodemodIdPair codemod =
        new CodemodIdPair("codemodder:java/inject-dep-1", new InjectsDependency1());
    ProjectProvider skippingProvider = mock(ProjectProvider.class);
    DependencyGAV skipped = DependencyGAV.createDefault("org.spring", "dep1", "1.1.1");
    when(skippingProvider.updateDependencies(any(), any(), any()))
        .thenReturn(
            DependencyUpdateResult.create(List.of(), List.of(skipped), List.of(), Set.of()));

    executor =
        new DefaultCodemodExecutor(
            repoDir,
            includesEverything,
            codemod,
            List.of(skippingProvider),
            List.of(),
            fileCache,
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create(),
            -1,
            -1,
            -1);
    CodeTFResult result = executor.execute(List.of(javaFile2));
    List<CodeTFChangesetEntry> firstChangeset = result.getChangeset();

    // should have changed the java file, but not the deps.txt file
    assertThat(firstChangeset).hasSize(1);
    assertThat(result.getCodemod()).isEqualTo("codemodder:java/inject-dep-1");
    assertThat(result.getFailedFiles()).isEmpty();
    CodeTFChangesetEntry javaFile2Entry = firstChangeset.get(0);
    assertThat(javaFile2Entry.getPath()).isEqualTo("Test2.java");
    assertThat(javaFile2Entry.getChanges()).hasSize(1);
    List<CodeTFPackageAction> packageActions =
        javaFile2Entry.getChanges().get(0).getPackageActions();

    assertThat(packageActions.size()).isEqualTo(1);
    CodeTFPackageAction packageAction = packageActions.get(0);
    assertThat(packageAction.getAction())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionType.ADD);
    assertThat(packageAction.getPackage()).isEqualTo("pkg:maven/org.spring/dep1@1.1.1");
    assertThat(packageAction.getResult())
        .isEqualTo(CodeTFPackageAction.CodeTFPackageActionResult.SKIPPED);
    assertThat(javaFile2Entry.getDiff())
        .isEqualTo(
            "--- Test2.java\n"
                + "+++ Test2.java\n"
                + "@@ -1,3 +1,5 @@\n"
                + " class Test2 {\n"
                + "-  void needsDep1() {}\n"
                + "+  void needsDep1() {\n"
                + "+      Dependency1.doStuff();\n"
                + "+  }\n"
                + " }");
  }

  private static void hasBeforeAfterCodemodMetadata(final CodeTFResult result) {
    assertThat(result.getCodemod()).isEqualTo("codemodder:java/id");
    assertThat(result.getDescription()).isEqualTo("before-after-description");
    assertThat(result.getSummary()).isEqualTo("before-after-summary");
  }

  private static void isJavaFile1ChangedCorrectly(final CodeTFChangesetEntry entry) {
    assertThat(entry.getPath()).isEqualTo("Test1.java");
    assertThat(entry.getChanges()).hasSize(1);
    CodeTFChange change = entry.getChanges().get(0);
    assertThat(change.getLineNumber()).isEqualTo(2);
    assertThat(change.getPackageActions()).isEmpty();
    assertThat(change.getDescription()).isEqualTo("some description");
    assertThat(entry.getDiff())
        .isEqualTo(
            "--- Test1.java\n"
                + "+++ Test1.java\n"
                + "@@ -1,3 +1,3 @@\n"
                + " class Test1 {\n"
                + "-  void before() {}\n"
                + "+  void after() {}\n"
                + " }");
  }

  private static void isJavaFile3ChangedCorrectly(final CodeTFChangesetEntry entry) {
    assertThat(entry.getPath()).isEqualTo("Test3.java");
    assertThat(entry.getChanges()).hasSize(1);
    CodeTFChange change = entry.getChanges().get(0);
    assertThat(change.getLineNumber()).isEqualTo(3);
    assertThat(change.getPackageActions()).isEmpty();
    assertThat(change.getDescription()).isEqualTo("some description");
    assertThat(entry.getDiff())
        .isEqualTo(
            "--- Test3.java\n"
                + "+++ Test3.java\n"
                + "@@ -1,4 +1,4 @@\n"
                + " class Test3 {\n"
                + "   void nothing() {}\n"
                + "-  void before() {}\n"
                + "+  void after() {}\n"
                + " }");
  }

  /** Changes all method calls to methods named "before" to "after". */
  private static class BeforeToAfterChanger extends JavaParserChanger {
    BeforeToAfterChanger() {
      super(new EmptyReporter());
    }

    @Override
    public List<CodemodChange> visit(
        final CodemodInvocationContext context, final CompilationUnit cu) {
      List<CodemodChange> changes = new ArrayList<>();
      cu.findAll(MethodDeclaration.class).stream()
          .filter(mb -> mb.getNameAsString().equals("before"))
          .forEach(
              mb -> {
                mb.setName("after");
                changes.add(CodemodChange.from(mb.getBegin().get().line));
              });
      return changes;
    }

    @Override
    public String getDescription() {
      return "before-after-description";
    }

    @Override
    public String getSummary() {
      return "before-after-summary";
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return List.of(
          new CodeTFReference("https://before-after-reference", "before-after-reference"));
    }

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return "some description";
    }
  }

  private static class InjectsDependency1 extends JavaParserChanger {

    InjectsDependency1() {
      super(new EmptyReporter());
    }

    @Override
    public List<CodemodChange> visit(CodemodInvocationContext context, CompilationUnit cu) {
      List<CodemodChange> changes = new ArrayList<>();
      cu.findAll(MethodDeclaration.class).stream()
          .filter(mb -> mb.getNameAsString().equals("needsDep1"))
          .forEach(
              mb -> {
                BlockStmt blockStmt = mb.getBody().get();
                blockStmt.addStatement("Dependency1.doStuff();");
                changes.add(CodemodChange.from(mb.getBegin().get().line, dependency1));
              });
      return changes;
    }

    @Override
    public String getDescription() {
      return "injects-dependency-1-description";
    }

    @Override
    public String getSummary() {
      return "injects-dependency-1-summary";
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return List.of(new CodeTFReference("https://dep1.com/", "https://dep1.com/"));
    }

    @Override
    public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
      return "injects-dependency-1-change";
    }
  }

  private static class InjectsDependency2 extends JavaParserChanger {

    InjectsDependency2() {
      super(new EmptyReporter());
    }

    @Override
    public List<CodemodChange> visit(CodemodInvocationContext context, CompilationUnit cu) {
      List<CodemodChange> changes = new ArrayList<>();
      cu.findAll(MethodDeclaration.class).stream()
          .filter(mb -> mb.getNameAsString().equals("needsDep2"))
          .forEach(
              mb -> {
                BlockStmt blockStmt = mb.getBody().get();
                blockStmt.addStatement("Dependency2.doStuff();");
                changes.add(CodemodChange.from(mb.getBegin().get().line, dependency2));
              });
      return changes;
    }

    @Override
    public String getDescription() {
      return "injects-dependency-2-description";
    }

    @Override
    public String getSummary() {
      return "injects-dependency-2-summary";
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return List.of(new CodeTFReference("https://dep2.com/", "https://dep2.com/"));
    }

    @Override
    public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
      return "injects-dependency-2-change";
    }
  }

  private static final DependencyGAV dependency1 =
      DependencyGAV.createDefault("org.spring", "dep1", "1.1.1");
  private static final DependencyGAV dependency2 =
      DependencyGAV.createDefault("org.apache", "dep2", "2.2.2");
}
