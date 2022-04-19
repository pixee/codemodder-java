package io.pixee.codefixer.java.protections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import io.pixee.security.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import io.pixee.codefixer.java.ChangedFile;
import io.pixee.codefixer.java.FileBasedVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.IncludesExcludes;
import io.pixee.codefixer.java.SourceDirectory;
import io.pixee.codefixer.java.SourceWeaver;
import io.pixee.codefixer.java.VisitorFactory;
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
    assertThat(vulnerableFile.exists(), is(true));
    assertThat(vulnerableFile.isFile(), is(true));

    final String pathToFixedFile = pathToVulnerableFile.replace(".java", "Weaved.java");
    var fixedFile = new File(pathToFixedFile);
    assertThat(fixedFile.exists(), is(true));
    assertThat(fixedFile.isFile(), is(true));

    List<VisitorFactory> visitorFactories = List.of(factory);
    var analyzer = SourceWeaver.createDefault();
    var testCodeDir = new File(vulnerableFile.getParent());
    var directory =
        SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToVulnerableFile));

    var changedFile =
        scanAndAssertNoErrorsWithOneFileChanged(analyzer, directory, visitorFactories, includesExcludes);
    var actualContents =
        FileUtils.readFileToString(new File(changedFile.modifiedFile()), Charset.defaultCharset());
    var expectedContents = FileUtils.readFileToString(fixedFile, Charset.defaultCharset());

    var typeName = vulnerableFile.getName().replace(".java", "");
    expectedContents =
        expectedContents.replace("final class " + typeName + "Weaved", "final class " + typeName);
    expectedContents =
        expectedContents.replace("interface " + typeName + "Weaved", "interface " + typeName);

    assertThat(actualContents, equalToIgnoringWhiteSpace(expectedContents));

    directory = SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToFixedFile));
    scanAndAssertNoErrorsWithNoFilesChanged(analyzer, directory, visitorFactories, includesExcludes);

    return changedFile;
  }

  public static ChangedFile assertJavaWeaveWorkedAndWontReweave(
      final String pathToVulnerableFile, final VisitorFactory factory) throws IOException {
    return assertJavaWeaveWorkedAndWontReweave(
        pathToVulnerableFile,
        factory,
        IncludesExcludes.fromConfiguration(
            new File(pathToVulnerableFile).getParentFile(),
            Collections.emptyList(),
            Collections.emptyList()));
  }

  private static ChangedFile scanAndAssertNoErrorsWithOneFileChanged(
          final SourceWeaver analyzer,
          final SourceDirectory directory,
          final List<VisitorFactory> visitorFactories,
          final IncludesExcludes includesExcludes)
      throws IOException {
    var weave = analyzer.weave(List.of(directory), visitorFactories, includesExcludes);
    assertThat(weave, is(not(nullValue())));
    assertThat(weave.unscannableFiles(), is(empty()));
    var changedFiles = weave.changedFiles();
    assertThat(changedFiles, hasSize(1));
    return changedFiles.iterator().next();
  }

  public static void scanAndAssertNoErrorsWithNoFilesChanged(
      final String pathToVulnerableFile,
      final VisitorFactory factory,
      final IncludesExcludes includesExcludes)
      throws IOException {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists(), is(true));
    assertThat(vulnerableFile.isFile(), is(true));
    List<VisitorFactory> visitorFactories = List.of(factory);
    var analyzer = SourceWeaver.createDefault();
    var testCodeDir = new File(vulnerableFile.getParent());
    var directory =
        SourceDirectory.createDefault(testCodeDir.getPath(), List.of(pathToVulnerableFile));
    scanAndAssertNoErrorsWithNoFilesChanged(analyzer, directory, visitorFactories, includesExcludes);
  }

  private static void scanAndAssertNoErrorsWithNoFilesChanged(
      final SourceWeaver analyzer,
      final SourceDirectory directory,
      final List<VisitorFactory> visitorFactory,
      final IncludesExcludes includesExcludes)
      throws IOException {
    var weave = analyzer.weave(List.of(directory), visitorFactory, includesExcludes);
    assertThat(weave, is(not(nullValue())));

    assertThat(weave.unscannableFiles(), is(empty()));

    var changedFiles = weave.changedFiles();
    assertThat(changedFiles, hasSize(0));
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
      final String pathToVulnerableFile, final FileBasedVisitor weaver) throws IOException {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists(), is(true));
    assertThat(vulnerableFile.isFile(), is(true));

    final String pathToFixedFile = pathToVulnerableFile.replace(".", "_weaved.");
    var fixedFile = new File(pathToFixedFile);
    assertThat(fixedFile.exists(), is(true));
    assertThat(fixedFile.isFile(), is(true));

    var result =
        weaver.visitRepositoryFile(
            new File(vulnerableFile.getParent()),
            vulnerableFile,
            FileWeavingContext.createDefault(
                vulnerableFile, new IncludesExcludes.MatchesEverything()),
            Collections.emptySet());
    assertThat(result.unscannableFiles().isEmpty(), is(true));
    var changedFiles = result.changedFiles();
    assertThat(changedFiles.size(), is(1));
    var changedFile = changedFiles.iterator().next();
    assertThat(
        new File(changedFile.originalFilePath()).getAbsolutePath(),
        equalTo(vulnerableFile.getAbsolutePath()));
    assertThat(
        FileUtils.readFileToString(new File(changedFile.modifiedFile())),
        equalTo(FileUtils.readFileToString(fixedFile)));
    return changedFile;
  }

  @NotNull
  public static void assertFileWeaveWorkedAndNoChangesNeeded(
      final String pathToVulnerableFile, final FileBasedVisitor weaver) throws IOException {
    var vulnerableFile = new File(pathToVulnerableFile);
    assertThat(vulnerableFile.exists(), is(true));
    assertThat(vulnerableFile.isFile(), is(true));

    var result =
        weaver.visitRepositoryFile(
            new File(vulnerableFile.getParent()),
            vulnerableFile,
            FileWeavingContext.createDefault(
                vulnerableFile, new IncludesExcludes.MatchesEverything()),
            Collections.emptySet());
    assertThat(result.unscannableFiles().isEmpty(), is(true));
    var changedFiles = result.changedFiles();
    assertThat(changedFiles.size(), is(0));
  }
}
