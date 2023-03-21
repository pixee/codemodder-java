package io.openpixee.java.protections;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.ChangedFile;
import io.codemodder.CodemodInvoker;
import io.codemodder.FileWeavingContext;
import io.codemodder.IncludesExcludes;
import io.codemodder.WeavingResult;
import io.codemodder.codemods.SecureRandomCodemod;
import io.openpixee.java.DoNothingVisitor;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.SourceDirectory;
import io.openpixee.java.SourceWeaver;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

/** Holds testing utilities. */
public abstract class WeavingTests {

  private WeavingTests() {}

  /**
   * This method takes the Java file given, asserts the presence of a "Weaved.java" file next to it,
   * and the given factory, and runs the following play:
   *
   * <p>1. Runs analysis and confirms that the vulnerable file was processed correctly. 2. Tests to
   * see if the weaved file is identical to the weaved sister file next to it. 3. Confirms that no
   * other files were somehow changed 4. Confirms that re-weaving the already-weaved file produces
   * no differences.
   */
  public static ChangedFile assertJavaWeaveWorkedAndWontReweave(
      final String pathToVulnerableFile,
      final VisitorFactory factory,
      final IncludesExcludes includesExcludes)
      throws IOException {

    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists()).isTrue();
    assertThat(vulnerableFile.isFile()).isTrue();

    final String pathToFixedFile = pathToVulnerableFile.replace(".java", "Weaved.java");
    var fixedFile = new File(pathToFixedFile);
    assertThat(fixedFile).isFile();

    List<VisitorFactory> visitorFactories = List.of(factory);

    var analyzer = SourceWeaver.createDefault();
    var testCodeDir = new File(vulnerableFile.getParent());
    var codemodInvoker =
        new CodemodInvoker(List.of(SecureRandomCodemod.class), testCodeDir.toPath());
    var directory =
        SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToVulnerableFile));

    var changedFile =
        scanAndAssertNoErrorsWithOneFileChanged(
            analyzer, directory, visitorFactories, codemodInvoker, includesExcludes);
    var expectedContents = FileUtils.readFileToString(fixedFile, Charset.defaultCharset());

    var typeName = vulnerableFile.getName().replace(".java", "");
    expectedContents =
        expectedContents.replace("final class " + typeName + "Weaved", "final class " + typeName);
    expectedContents =
        expectedContents.replace("interface " + typeName + "Weaved", "interface " + typeName);

    assertThat(new File(changedFile.modifiedFile()))
        .content()
        .isEqualToIgnoringWhitespace(expectedContents);

    directory = SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToFixedFile));
    scanAndAssertNoErrorsWithNoFilesChanged(
        analyzer, directory, visitorFactories, codemodInvoker, includesExcludes);

    return changedFile;
  }

  /**
   * This overload is for facilitating the migration from the old legacy {@link VisitorFactory}
   * strategy to the {@link io.codemodder.Codemod} strategy. This follows all the same testing path,
   * except it uses a dummy visitor that won't do anything useful, so we can be sure that the
   * changes that occur due to codemods.
   */
  public static ChangedFile assertJavaWeaveWorkedAndWontReweave(final String pathToVulnerableFile)
      throws IOException {
    return assertJavaWeaveWorkedAndWontReweave(
        pathToVulnerableFile,
        new VisitorFactory() {
          @Override
          public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
              final File file, final CompilationUnit cu) {
            return new DoNothingVisitor();
          }

          @Override
          public String ruleId() {
            return "pixee:java/unused";
          }
        });
  }

  public static ChangedFile assertJavaWeaveWorkedAndWontReweave(
      final String pathToVulnerableFile, final VisitorFactory factory) throws IOException {
    return assertJavaWeaveWorkedAndWontReweave(
        pathToVulnerableFile,
        factory,
        IncludesExcludes.withSettings(
            new File(pathToVulnerableFile).getParentFile(),
            Collections.emptyList(),
            Collections.emptyList()));
  }

  private static ChangedFile scanAndAssertNoErrorsWithOneFileChanged(
      final SourceWeaver analyzer,
      final SourceDirectory directory,
      final List<VisitorFactory> visitorFactories,
      final CodemodInvoker codemodInvoker,
      final IncludesExcludes includesExcludes)
      throws IOException {
    var weave =
        analyzer.weave(
            new File("src/test"),
            List.of(directory),
            directory.files(),
            visitorFactories,
            codemodInvoker,
            includesExcludes);
    assertThat(weave).isNotNull();
    assertThat(weave.unscannableFiles()).isEmpty();
    var changedFiles = weave.changedFiles();
    assertThat(changedFiles).hasSize(1);
    return changedFiles.iterator().next();
  }

  public static void scanAndAssertNoErrorsWithNoFilesChanged(
      final String pathToVulnerableFile,
      final VisitorFactory factory,
      final IncludesExcludes includesExcludes)
      throws IOException {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists()).isTrue();
    assertThat(vulnerableFile.isFile()).isTrue();
    List<VisitorFactory> visitorFactories = List.of(factory);
    var analyzer = SourceWeaver.createDefault();
    var testCodeDir = new File(vulnerableFile.getParent());
    var codemodInvoker =
        new CodemodInvoker(List.of(SecureRandomCodemod.class), testCodeDir.toPath());
    var directory =
        SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToVulnerableFile));

    scanAndAssertNoErrorsWithNoFilesChanged(
        analyzer, directory, visitorFactories, codemodInvoker, includesExcludes);
  }

  private static void scanAndAssertNoErrorsWithNoFilesChanged(
      final SourceWeaver analyzer,
      final SourceDirectory directory,
      final List<VisitorFactory> visitorFactories,
      final CodemodInvoker codemodInvoker,
      final IncludesExcludes includesExcludes)
      throws IOException {
    var weave =
        analyzer.weave(
            new File("src/test"),
            List.of(directory),
            directory.files(),
            visitorFactories,
            codemodInvoker,
            includesExcludes);
    assertThat(weave).isNotNull();

    assertThat(weave.unscannableFiles()).isEmpty();

    var changedFiles = weave.changedFiles();
    assertThat(changedFiles).hasSize(0);
  }

  /**
   * This overload is for facilitating the migration from the old legacy {@link FileBasedVisitor}
   * strategy to the {@link io.codemodder.Codemod} strategy. This follows all the same testing path,
   * except it uses a dummy visitor that won't do anything useful, so we can be sure that the
   * changes that occur due to codemods.
   */
  public static ChangedFile assertFileWeaveWorkedAndReweave(final String pathToVulnerableFile) {
    return assertFileWeaveWorkedAndReweave(
        pathToVulnerableFile,
        new FileBasedVisitor() {
          @Override
          public WeavingResult visitRepositoryFile(
              final File repositoryRoot,
              final File file,
              final FileWeavingContext weavingContext,
              final Set<ChangedFile> changedJavaFiles) {
            return WeavingResult.empty();
          }

          @Override
          public String ruleId() {
            return "pixee:java/does-nothing";
          }
        });
  }

  /**
   * This method takes the plaintext file given, asserts the presence of a "_weaved.[ext]" file next
   * to it, and the given weaver, and runs the following play:
   *
   * <p>1. Runs analysis and confirms that the vulnerable file was processed correctly. 2. Tests to
   * see if the weaved file is identical to the weaved sister file next to it. 3. Confirms that no
   * other files were somehow changed 4. Confirms that re-weaving the already-weaved file produces
   * no differences.
   */
  @NotNull
  public static ChangedFile assertFileWeaveWorkedAndReweave(
      final String pathToVulnerableFile, final FileBasedVisitor weaver) {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists()).isTrue();
    assertThat(vulnerableFile.isFile()).isTrue();

    final String pathToFixedFile = pathToVulnerableFile.replace(".", "_weaved.");
    var fixedFile = new File(pathToFixedFile);
    assertThat(fixedFile).isFile();

    var result =
        weaver.visitRepositoryFile(
            new File(vulnerableFile.getParent()),
            vulnerableFile,
            FileWeavingContext.createDefault(
                vulnerableFile, new IncludesExcludes.MatchesEverything()),
            Collections.emptySet());
    assertThat(result.unscannableFiles()).isEmpty();
    var changedFiles = result.changedFiles();
    assertThat(changedFiles).hasSize(1);
    var changedFile = changedFiles.iterator().next();
    assertThat(new File(changedFile.originalFilePath()).getAbsolutePath())
        .isEqualTo(vulnerableFile.getAbsolutePath());
    assertThat(new File(changedFile.modifiedFile())).hasSameTextualContentAs(fixedFile);
    return changedFile;
  }

  @NotNull
  public static void assertFileWeaveWorkedAndNoChangesNeeded(
      final String pathToVulnerableFile, final FileBasedVisitor weaver) {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists()).isTrue();
    assertThat(vulnerableFile.isFile()).isTrue();

    var result =
        weaver.visitRepositoryFile(
            new File(vulnerableFile.getParent()),
            vulnerableFile,
            FileWeavingContext.createDefault(
                vulnerableFile, new IncludesExcludes.MatchesEverything()),
            Collections.emptySet());
    assertThat(result.unscannableFiles()).isEmpty();
    var changedFiles = result.changedFiles();
    assertThat(changedFiles).isEmpty();
  }
}
