package io.codemodder.plugins.maven.operator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MassRepoIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(MassRepoIT.class);

  private static class TestRepo {
    private String slug;
    private String branch;
    private String pomPath;
    private boolean useProperties;
    private boolean useScanner;
    private String commitId;

    TestRepo(
        String slug,
        boolean useProperties,
        String branch,
        boolean useScanner,
        String commitId,
        String pomPath) {
      this.slug = slug;
      this.useProperties = useProperties;
      this.branch = branch != null ? branch : "master";
      this.useScanner = useScanner;
      this.commitId = commitId;
      this.pomPath = pomPath != null ? pomPath : "pom.xml";
    }

    Path cacheDir() {
      String cacheDirName =
          BASE_CACHE_DIR + File.separator + "repo-" + String.format("%08X", slug.hashCode());
      return Paths.get(cacheDirName);
    }

    static final File BASE_CACHE_DIR =
        new File(System.getProperty("user.dir") + "/.cache").getAbsoluteFile();
  }

  private final List<Pair<TestRepo, String>> repos =
      Arrays.asList(
          new Pair(
              new TestRepo(
                  "WebGoat/WebGoat",
                  true,
                  "main",
                  false,
                  "e75cfbeb110e3d3a2ca3c8fee2754992d89c419d",
                  "webgoat-lessons/xxe/pom.xml"),
              "io.github.pixee:java-security-toolkit:1.0.2"),
          new Pair(
              new TestRepo(
                  "WebGoat/WebGoat", true, "main", true, null, "webgoat-container/pom.xml"),
              "io.github.pixee:java-security-toolkit:1.0.2"),
          new Pair(
              new TestRepo(
                  "WebGoat/WebGoat", true, "main", false, null, "webgoat-container/pom.xml"),
              "io.github.pixee:java-security-toolkit:1.0.2"),
          new Pair(
              new TestRepo(
                  "WebGoat/WebGoat", true, "main", false, null, "webgoat-container/pom.xml"),
              "io.github.pixee:java-security-toolkit:1.0.2"),
          new Pair(
              new TestRepo("CRRogo/vert.x", true, null, false, null, null),
              "io.github.pixee:java-security-toolkit:1.0.2"),
          new Pair(
              new TestRepo("apache/pulsar", false, null, false, null, "pulsar-broker/pom.xml"),
              "commons-codec:commons-codec:1.14"),
          new Pair(
              new TestRepo("apache/rocketmq", false, null, false, null, "common/pom.xml"),
              "commons-codec:commons-codec:1.15"),
          new Pair(
              new TestRepo(
                  "OpenAPITools/openapi-generator",
                  false,
                  null,
                  false,
                  null,
                  "modules/openapi-generator-core/pom.xml"),
              "com.google.guava:guava:31.0-jre"),
          new Pair(
              new TestRepo("casbin/jcasbin", false, null, false, null, null),
              "com.google.code.gson:gson:2.8.0"),
          new Pair(
              new TestRepo("bytedeco/javacv", false, null, false, null, null),
              "org.jogamp.jocl:jocl-main:2.3.1"));

  private void checkoutOrResetCachedRepo(TestRepo repo) throws IOException, InterruptedException {
    LOGGER.info("Checkout out {} into {}", repo.slug, repo.cacheDir());

    if (Files.notExists(repo.cacheDir())) {
      String[] command = {
        "git",
        "clone",
        "-b",
        repo.branch,
        "https://github.com/" + repo.slug + ".git",
        repo.cacheDir().toRealPath().toString()
      };

      LOGGER.debug("Running command: " + String.join(" ", command));

      Process process =
          new ProcessBuilder(command)
              .directory(TestRepo.BASE_CACHE_DIR.getCanonicalFile())
              .inheritIO()
              .start();

      process.waitFor();
    } else {
      String[] command = {"git", "reset", "--hard", "HEAD"};

      LOGGER.debug("Running command: " + String.join(" ", command));

      Process process =
          new ProcessBuilder(command).directory(repo.cacheDir().toFile()).inheritIO().start();

      process.waitFor();
    }

    if (repo.commitId != null) {
      String[] command = {"git", "checkout", repo.commitId};

      LOGGER.debug("Running command: " + String.join(" ", command));

      Process process =
          new ProcessBuilder(command).directory(repo.cacheDir().toFile()).inheritIO().start();

      process.waitFor();
    }
  }

  private String getDependenciesFrom(TestRepo repo) throws Exception {
    try {
      return getDependenciesFrom(repo.pomPath, repo.cacheDir());
    } catch (Exception e) {
      Path pomFile = repo.cacheDir().resolve(repo.pomPath);

      final POMOperator pomOperator = new POMOperator(pomFile, repo.cacheDir());

      Collection<Dependency> dependencies =
          pomOperator.queryDependency(
              pomOperator.getPomScanner().scanFrom().withRepositoryPath(repo.cacheDir()).build());

      StringBuilder result = new StringBuilder();

      for (Dependency dependency : dependencies) {
        result.append(dependency.toString()).append("\n");
      }

      return result.toString();
    }
  }

  private String getDependenciesFrom(String pomPath, Path dir)
      throws IOException, InterruptedException {
    File outputFile = File.createTempFile("tmp-pom", ".txt");

    if (outputFile.exists()) {
      outputFile.delete();
    }

    String[] command;
    if (SystemUtils.IS_OS_WINDOWS) {
      command = new String[] {Util.which("cmd").getCanonicalPath(), "/c"};
    } else {
      command = new String[] {};
    }
    String[] commands =
        new String[] {
          Util.which("mvn").getCanonicalPath(),
          "-B",
          "-f",
          pomPath,
          "dependency:tree",
          "-Dscope=runtime",
          "-DoutputFile=" + outputFile.getCanonicalPath()
        };

    List<String> commandList = new ArrayList<>(command.length + commands.length);
    Collections.addAll(commandList, commands);
    Collections.addAll(commandList, command);

    LOGGER.debug("Running: " + String.join(" ", commandList));
    LOGGER.debug("Dir: " + dir);

    Process process = new ProcessBuilder(commandList).directory(dir.toFile()).inheritIO().start();

    process.waitFor();

    if (!outputFile.exists()) {
      return "";
    }

    String result = null;
    try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))) {
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append(System.lineSeparator());
      }
      result = stringBuilder.toString();
    } catch (IOException e) {
      // Handle any IOException that may occur during file reading
      e.printStackTrace();
    }

    if (!outputFile.delete()) {
      LOGGER.warn("Failed to delete temporary file: {}", outputFile);
    }

    return result;
  }

  @Test
  void testBasic() throws Exception {
    Pair<TestRepo, String> firstCase = repos.get(0);

    testOnRepo(firstCase.first, firstCase.second);
  }

  @Test
  void testAllOthers() {
    for (int n = 0; n < repos.size(); n++) {
      Pair<TestRepo, String> pair = repos.get(n);
      try {
        testOnRepo(pair.first, pair.second);
      } catch (Throwable e) {
        throw new AssertionError("while trying example " + n + " of " + pair, e);
      }
    }
  }

  private void testOnRepo(TestRepo sampleRepo, String dependencyToUpgradeString) throws Exception {
    LOGGER.info(
        "Testing on repo {}, branch {} with dependency {} ({})",
        sampleRepo.slug,
        sampleRepo.branch,
        dependencyToUpgradeString,
        sampleRepo);

    checkoutOrResetCachedRepo(sampleRepo);

    String originalDependencies = getDependenciesFrom(sampleRepo);

    LOGGER.info("dependencies: {}", originalDependencies);

    Dependency dependencyToUpgrade = Dependency.fromString(dependencyToUpgradeString);

    final POMOperator pomOperator =
        new POMOperator(sampleRepo.cacheDir().resolve(sampleRepo.pomPath), sampleRepo.cacheDir());

    final POMScanner pomScanner = pomOperator.getPomScanner();

    ProjectModelFactory projectModelFactory =
        sampleRepo.useScanner
            ? pomScanner.scanFrom()
            : ProjectModelFactory.load(
                Paths.get(sampleRepo.cacheDir().toString(), sampleRepo.pomPath));

    ProjectModel context =
        projectModelFactory
            .withDependency(dependencyToUpgrade)
            .withSkipIfNewer(false)
            .withUseProperties(sampleRepo.useProperties)
            .build();

    boolean result = pomOperator.modify(context);

    context.allPomFiles().stream()
        .filter(pomFile -> pomFile.getDirty())
        .forEach(
            pomFile -> {
              try (OutputStream fos = Files.newOutputStream(pomFile.getFile())) {
                fos.write(pomFile.getResultPomBytes());
              } catch (IOException e) {
                // Handle any IOException that may occur during writing
                e.printStackTrace();
              }
            });

    String finalDependencies = getDependenciesFrom(sampleRepo);

    LOGGER.info("dependencies: {}", finalDependencies);

    boolean queryFailed = originalDependencies.isEmpty() && finalDependencies.isEmpty();

    if (queryFailed) {
      Assert.assertTrue("Must be modified even when query failed", result);
    } else {
      String dependencyAsStringWithPackaging = dependencyToUpgrade.toString();

      Assert.assertFalse(
          "Dependency should be originally missing",
          originalDependencies.contains(dependencyAsStringWithPackaging));
      Assert.assertTrue(
          "New Dependency should be appearing",
          finalDependencies.contains(dependencyAsStringWithPackaging));
    }
  }

  static class Pair<TestRepo, String> {
    TestRepo first;
    String second;

    Pair(TestRepo first, String second) {
      this.first = first;
      this.second = second;
    }
  }

  static {
    // Creates the Cache Directory
    if (!TestRepo.BASE_CACHE_DIR.exists()) {
      TestRepo.BASE_CACHE_DIR.mkdirs();
    }
  }
}
