package io.openpixee.java.protections;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class GitRepositoryTest {

  protected String repoURI;

  protected String repoBranch;

  protected String tempDirName;

  /** Hash string of the commit the repo should be at. */
  protected String refHash;

  /** Shared repo dir for all tests. */
  protected File repoDir;

  /** The output file for each test. */
  protected File outputFile;

  protected static boolean isCached(File dir) throws IOException {
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

  /** Must set repoURI, repoBranch, tempDirName at least */
  abstract void setParameters();

  @BeforeAll
  protected void setup() throws GitAPIException, IOException {
    setParameters();

    String tmpDir = System.getProperty("java.io.tmpdir");
    repoDir = new File(tmpDir, tempDirName);

    if (!isCached(repoDir)) {
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
    this.outputFile = File.createTempFile("report", ".log");
    outputFile.deleteOnExit();
  }
}
