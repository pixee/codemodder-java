package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultSemgrepRunner implements SemgrepRunner {

  private final ObjectMapper objectMapper;

  DefaultSemgrepRunner() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public SarifSchema210 run(final List<Path> ruleYamls, final Path repository) throws IOException {
    Path repositoryPath = repository.toAbsolutePath();
    Path sarifFile = Files.createTempFile("semgrep", ".sarif");

    LOG.debug("Repository: {}", dumpInfo(repositoryPath));
    List<String> args = new ArrayList<>();
    args.add("semgrep");
    args.add("--no-error");
    args.add("--dataflow-traces");
    args.add("--sarif");
    args.add("-o");
    args.add(sarifFile.toAbsolutePath().toString());

    for (Path ruleYamlPath : ruleYamls) {
      args.add("--config");
      args.add(ruleYamlPath.toString());
    }
    args.add(repositoryPath.toString());

    LOG.debug("Process arguments: {}", args);
    /*
     * Create an empty directory to be the working directory, and add an .semgrepignore file that allows scanning
     * everything. If we don't do this, Semgrep will use its defaults which exclude a lot of stuff we want to scan.
     */
    Path tmpDir = Files.createTempDirectory("codemodder-semgrep");
    Path semgrepIgnoreFile = Files.createFile(tmpDir.resolve(".semgrepignore"));
    Files.write(semgrepIgnoreFile, OUR_SEMGREPIGNORE_CONTENTS.getBytes(StandardCharsets.UTF_8));

    LOG.debug("Will execute Semgrep from this directory: {}", dumpInfo(tmpDir));
    LOG.debug("Semgrep ignore file is located at: {}", dumpInfo(semgrepIgnoreFile));
    LOG.debug("SARIF file will be located at: {}", dumpInfo(sarifFile));

    Path semgrepHome = Files.createTempDirectory("semgrep-home").toAbsolutePath();
    LOG.debug("Semgrep home will be: {}", dumpInfo(semgrepHome));
    ProcessBuilder pb = new ProcessBuilder(args);
    pb.environment().put("HOME", semgrepHome.toString());
    Process p = pb.directory(tmpDir.toFile()).inheritIO().start();
    try {
      int rc = p.waitFor();
      LOG.debug("Semgrep return code: {}", rc);
      if (rc != 0) {
        throw new RuntimeException("error code seen from semgrep execution: " + rc);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("problem waiting for semgrep process execution", e);
    }

    SarifSchema210 sarif =
        objectMapper.readValue(Files.newInputStream(sarifFile), SarifSchema210.class);
    LOG.debug("SARIF results: {}", sarif.getRuns().get(0).getResults().size());
    Files.delete(semgrepIgnoreFile);
    Files.delete(tmpDir);
    Files.delete(sarifFile);
    return sarif;
  }

  private String dumpInfo(final Path path) {
    File file = path.toFile();
    return path + " (canRead=" + file.canRead() + ", canWrite=" + file.canWrite() + ")";
  }

  private static final String OUR_SEMGREPIGNORE_CONTENTS = "# dont ignore anything";
  private static final Logger LOG = LoggerFactory.getLogger(DefaultSemgrepRunner.class);
}
