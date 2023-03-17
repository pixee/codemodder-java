package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class DefaultSemgrepRunner implements SemgrepRunner {

  private final ObjectMapper objectMapper;

  DefaultSemgrepRunner() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public SarifSchema210 run(final List<Path> ruleYamls, final Path repository) throws IOException {
    String repositoryPath = repository.toString();
    Path sarifFile = Files.createTempFile("semgrep", ".sarif");

    List<String> args = new ArrayList<>();
    args.add("semgrep");
    args.add("--sarif");
    args.add("-o");
    args.add(sarifFile.toAbsolutePath().toString());
    for (Path ruleYamlPath : ruleYamls) {
      args.add("--config");
      args.add(ruleYamlPath.toString());
    }
    args.add(repositoryPath);

    // create an empty .semgrepignore file
    Path tmpDir = Files.createTempDirectory("codemodder-semgrep");
    Path semgrepIgnoreFile = Files.createFile(tmpDir.resolve(".semgrepignore"));
    Files.write(semgrepIgnoreFile, OUR_SEMGREPIGNORE_CONTENTS.getBytes(StandardCharsets.UTF_8));

    Process p = new ProcessBuilder(args).inheritIO().start();
    try {
      int rc = p.waitFor();
      if (rc != 0) {
        throw new RuntimeException("error code seen from semgrep execution: " + rc);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("problem waiting for semgrep process execution", e);
    }


    SarifSchema210 sarif =
        objectMapper.readValue(Files.newInputStream(sarifFile), SarifSchema210.class);
    Files.delete(semgrepIgnoreFile);
    Files.delete(tmpDir);
    Files.delete(sarifFile);
    return sarif;
  }

  private static final String OUR_SEMGREPIGNORE_CONTENTS = "# dont ignore anything";
}
