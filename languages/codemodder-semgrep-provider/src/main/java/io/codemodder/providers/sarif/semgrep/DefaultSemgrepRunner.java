package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@inheritDoc} */
final class DefaultSemgrepRunner implements SemgrepRunner {

  /** {@inheritDoc} */
  @Override
  public SarifSchema210 run(final List<Path> ruleYamls, final Path repository) throws IOException {
    String repositoryPath = repository.toString();
    File sarifFile = File.createTempFile("semgrep", ".sarif");
    sarifFile.deleteOnExit();

    List<String> args = new ArrayList<>();
    args.add("semgrep");
    args.add("--sarif");
    args.add("-o");
    args.add(sarifFile.getAbsolutePath());
    for (Path ruleYamlPath : ruleYamls) {
      args.add("--config");
      args.add(ruleYamlPath.toString());
    }
    args.add(repositoryPath);

    // backup existing .segmrepignore if it exists
    File existingSemgrepFile = new File(".semgrepignore");
    Optional<File> backup = Optional.empty();

    if (existingSemgrepFile.exists()) {
      File backupFile = File.createTempFile("backup", ".semgrepignore");
      if (backupFile.exists()) {
        backupFile.delete(); // i don't know how but this is happening in tests
      }
      Files.copy(existingSemgrepFile.toPath(), backupFile.toPath());
      backup = Optional.of(backupFile);
    }

    // create an an empty .semgrepignore file
    Files.write(
        existingSemgrepFile.toPath(), OUR_SEMGREPIGNORE_CONTENTS.getBytes(StandardCharsets.UTF_8));

    Process p = new ProcessBuilder(args).inheritIO().start();
    try {
      int rc = p.waitFor();
      if (rc != 0) {
        throw new RuntimeException("error code seen from semgrep execution: " + rc);
      }
    } catch (InterruptedException e) {
      logger.error("problem waiting for semgrep process execution", e);
    }

    // restore existing .semgrepignore if it exists
    if (backup.isPresent()) {
      Files.copy(
          backup.get().toPath(), existingSemgrepFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } else {
      existingSemgrepFile.delete();
    }
    return new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class);
  }

  private static final String OUR_SEMGREPIGNORE_CONTENTS = "# dont ignore anything";

  private static final Logger logger = LoggerFactory.getLogger(SemgrepRunner.class);
}
