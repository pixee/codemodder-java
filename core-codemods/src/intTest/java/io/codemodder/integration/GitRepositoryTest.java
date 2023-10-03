package io.codemodder.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
}
