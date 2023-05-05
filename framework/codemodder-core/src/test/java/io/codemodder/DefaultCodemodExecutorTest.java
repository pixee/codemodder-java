package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.codetf.*;
import io.codemodder.javaparser.CachingJavaParser;
import io.codemodder.javaparser.JavaParserChanger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link DefaultCodemodExecutor} that ensure CodeTF is generated as expected. */
final class DefaultCodemodExecutorTest {

  private JavaParserChanger beforeToAfterChanger;
  private Path repoDir;
  private DefaultCodemodExecutor executor;
  private CodemodIdPair beforeAfterCodemod;
  private Path javaFile1;
  private Path javaFile2;
  private Path javaFile3;
  private Path javaFile4;
  private Path depsFile;

  @BeforeEach
  void setup(final @TempDir Path tmpDir) throws IOException {
    this.repoDir = tmpDir;
    this.beforeToAfterChanger = new BeforeToAfterChanger();
    beforeAfterCodemod = new CodemodIdPair("codemodder:java/id", beforeToAfterChanger);
    executor =
        new DefaultCodemodExecutor(
            repoDir,
            IncludesExcludes.any(),
            beforeAfterCodemod,
            List.of(),
            CachingJavaParser.from(new JavaParser()),
            EncodingDetector.create());

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
    depsFile = repoDir.resolve("deps.txt");
    Files.write(depsFile, List.of("my-org:my-existing-dependency:1.0.0"));
  }

  /**
   * This type provides dependency injection features for an imaginary dependency mgmt tool called
   * "deps". It simply add the dependencies, one per line, to the bottom of a dep.txt file at the
   * root of the project, with no checking.
   */
  static class FakeDepsProvider implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        final Path projectDir, final Path file, final List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      Path deps = projectDir.resolve("deps.txt");
      List<String> oldDeps = Files.readAllLines(deps);
      List<String> newDeps =
          remainingFileDependencies.stream()
              .map(dep -> dep.group() + ":" + dep.artifact() + ":" + dep.version())
              .collect(Collectors.toList());
      List<String> allDeps = new ArrayList<>();
      allDeps.addAll(oldDeps);
      allDeps.addAll(newDeps);
      Files.writeString(deps, String.join("\n", allDeps));

      List<String> patchDiff =
          UnifiedDiffUtils.generateUnifiedDiff(
              deps.getFileName().toString(),
              deps.getFileName().toString(),
              oldDeps,
              DiffUtils.diff(oldDeps, allDeps),
              3);
      String diff = String.join("\n", patchDiff);

      CodeTFChange change =
          new CodeTFChange(oldDeps.size() + 1, Collections.emptyMap(), "updated deps", List.of());
      CodeTFChangesetEntry entry = new CodeTFChangesetEntry("deps.txt", diff, List.of(change));
      Set<CodeTFChangesetEntry> changes = Set.of(entry);
      return DependencyUpdateResult.create(remainingFileDependencies, changes, Set.of());
    }
  }

  @Test
  void it_generates_single_codemod_codetf() {
    CodeTFResult result = executor.execute(List.of(javaFile1));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBasicCodemodStuff);

    // should have just 1 entry because we only scanned javaFile1
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(1);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile1ChangedCorrectly);
  }

  @Test
  void it_generates_all_files_codemod_codetf() {
    CodeTFResult result = executor.execute(List.of(javaFile1, javaFile2, javaFile3));
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBasicCodemodStuff);
    assertThat(result).satisfies(DefaultCodemodExecutorTest::hasBasicCodemodStuff);

    // should have 2 entries for both javaFile1 and javaFile3
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size()).isEqualTo(2);
    assertThat(changeset.get(0)).satisfies(DefaultCodemodExecutorTest::isJavaFile1ChangedCorrectly);
    assertThat(changeset.get(1)).satisfies(DefaultCodemodExecutorTest::isJavaFile3ChangedCorrectly);
  }

  @Test
  void it_handles_multiple_file_and_dependency_changes() throws IOException {
    List<CodemodIdPair> codemods = new ArrayList<>();
    codemods.add(new CodemodIdPair("codemodder:java/inject-dep-1", new InjectsDependency1()));
    codemods.add(new CodemodIdPair("codemodder:java/inject-dep-2", new InjectsDependency2()));
    FakeDepsProvider depsProvider = new FakeDepsProvider();
    List<CodeTFResult> results = new ArrayList<>();
    for (CodemodIdPair codemod : codemods) {
      executor =
          new DefaultCodemodExecutor(
              repoDir,
              IncludesExcludes.any(),
              codemod,
              List.of(depsProvider),
              CachingJavaParser.from(new JavaParser()),
              EncodingDetector.create());
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
    assertThat(injectDependency1Result.getCodemodId()).isEqualTo("codemodder:java/inject-dep-1");
    assertThat(injectDependency1Result.getFailedFiles()).isEmpty();
    CodeTFChangesetEntry javaFile2Entry = firstChangeset.get(0);
    assertThat(javaFile2Entry.getPath()).isEqualTo("Test2.java");
    assertThat(javaFile2Entry.getChanges()).hasSize(1);
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
                + "+org.spring:dep1:1.1.1");

    CodeTFResult injectDependency2Result = report.getResults().get(1);
    List<CodeTFChangesetEntry> secondChangeset = injectDependency2Result.getChangeset();
    assertThat(secondChangeset).hasSize(2);
    assertThat(injectDependency2Result.getCodemodId()).isEqualTo("codemodder:java/inject-dep-2");
    assertThat(injectDependency2Result.getFailedFiles()).isEmpty();
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

    CodeTFChangesetEntry secondDepsEntry = secondChangeset.get(1);
    assertThat(secondDepsEntry.getPath()).isEqualTo("deps.txt");
    assertThat(secondDepsEntry.getDiff())
        .isEqualTo(
            "--- deps.txt\n"
                + "+++ deps.txt\n"
                + "@@ -1,2 +1,3 @@\n"
                + " my-org:my-existing-dependency:1.0.0\n"
                + " org.spring:dep1:1.1.1\n"
                + "+org.apache:dep2:2.2.2");

    // assert that the report can be serialized by jackson to json without error
    ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
    String codetf = writer.writeValueAsString(report);
    assertThat(codetf).isNotBlank();
  }

  private static void hasBasicCodemodStuff(final CodeTFResult result) {
    assertThat(result.getCodemodId()).isEqualTo("codemodder:java/id");
    assertThat(result.getDescription()).isEqualTo("before-after-description");
    assertThat(result.getSummary()).isEqualTo("before-after-summary");
  }

  private static void isJavaFile1ChangedCorrectly(final CodeTFChangesetEntry entry) {
    assertThat(entry.getPath()).isEqualTo("Test1.java");
    assertThat(entry.getChanges()).hasSize(1);
    CodeTFChange change = entry.getChanges().get(0);
    assertThat(change.getLineNumber()).isEqualTo(2);
    assertThat(change.getDependencies()).isEmpty();
    assertThat(change.getDescription()).isEqualTo("");
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
    assertThat(change.getDependencies()).isEmpty();
    assertThat(change.getDescription()).isEqualTo("");
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
  private static class BeforeToAfterChanger implements JavaParserChanger {
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
  }

  private static class InjectsDependency1 implements JavaParserChanger {
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
  }

  private static class InjectsDependency2 implements JavaParserChanger {
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
  }

  private static final DependencyGAV dependency1 =
      DependencyGAV.createDefault("org.spring", "dep1", "1.1.1");
  private static final DependencyGAV dependency2 =
      DependencyGAV.createDefault("org.apache", "dep2", "2.2.2");
}
