package io.codemodder.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeEach;

// @TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class GitRepositoryTest {

  private String repoURI;

  private String repoBranch;

  private String tempDirName;

  /** Hash string of the commit the repo should be at. */
  private String refHash;

  /** Shared repo dir for all tests. */
  protected File repoDir;

  /** The output file for each test. */
  protected File outputFile;

  protected GitRepositoryTest(final String repoURI, final String repoBranch, final String refHash) {
    this.repoURI = Objects.requireNonNull(repoURI);
    this.repoBranch = Objects.requireNonNull(repoBranch);
    this.tempDirName = getClass().getSimpleName();
    this.refHash = Objects.requireNonNull(refHash);
  }

  private static boolean isCached(File dir) throws IOException {
    var rb = new FileRepositoryBuilder();
    rb.findGitDir(dir);
    if (rb.getGitDir() == null) return false;
    var rep = rb.build();
    for (var ref : rep.getAllRefs().values()) {
      if (ref.getObjectId() == null) continue;
      return true;
    }
    return false;
  }

  @BeforeEach
  protected void setup() throws GitAPIException, IOException {
    String tmpDir = System.getProperty("java.io.tmpdir");
    repoDir = new File(tmpDir, tempDirName);

    if (!isCached(repoDir)) {
      // safeguard to ensure that we can actually clone
      if (repoDir.exists()) {
        try (Stream<Path> pathStream = Files.walk(repoDir.toPath())) {
          for (var it = pathStream.sorted(Comparator.reverseOrder()).iterator(); it.hasNext(); ) {
            var p = it.next();
            Files.delete(p);
          }
        }
      }
      var git =
          Git.cloneRepository().setURI(repoURI).setDirectory(repoDir).setBranch(repoBranch).call();
      git.close();
      System.out.println("Writing to " + repoDir.getAbsolutePath());
      if (refHash != null) Git.open(repoDir).reset().setRef(refHash).setMode(ResetType.HARD).call();
    } else { // Repo has been cloned locally - do a hard reset instead
      Git.open(repoDir).reset().setMode(ResetCommand.ResetType.HARD).call();
    }
  }

  @BeforeEach
  void createOutputFile() throws IOException {
    this.outputFile = Files.createTempFile("report", ".log").toFile();
    outputFile.deleteOnExit();
  }

  protected void verifyNoFailedFiles(final CodeTFReport report) {
    List<String> failedFiles =
        report.getResults().stream()
            .map(CodeTFResult::getFailedFiles)
            .flatMap(Collection::stream)
            .toList();
    if (!failedFiles.isEmpty()) {
      System.out.println("Failed files during scan:");
      failedFiles.forEach(System.err::println);
    }
    int size = failedFiles.size();
    assertThat(size, is(0));
  }

  protected void verifyStandardCodemodResults(final List<CodeTFChangesetEntry> fileChanges) {
    // we only inject into a couple files
    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("SerializationHelper.java")),
        is(true));

    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("InsecureDeserializationTask.java")),
        is(true));
  }

  protected void verifyCodemodsHitWithChangesetCount(
      final CodeTFReport report, final String codemodId, final int changes) {
    List<CodeTFResult> results =
        report.getResults().stream()
            .filter(result -> codemodId.equals(result.getCodemod()))
            .toList();
    assertThat(results.size(), equalTo(1)); // should only have 1 entry per codemod
    assertThat(results.get(0).getChangeset().size(), equalTo(changes));
  }

  protected static final String testPathIncludes =
      "**.java,"
          + "**/*.java,"
          + "pom.xml,"
          + "**/pom.xml,"
          + "**.jsp,"
          + "**/*.jsp,"
          + "web.xml,"
          + "**/web.xml,"
          + ".github/workflows/*.yml,"
          + ".github/workflows/*.yaml";

  protected static final String testPathExcludes =
      "**/test/**,"
          + "**/testFixtures/**,"
          + "**/*Test.java,"
          + "**/intTest/**,"
          + "**/tests/**,"
          + "**/target/**,"
          + "**/build/**,"
          + "**/.mvn/**,"
          + ".mvn/**";
}
