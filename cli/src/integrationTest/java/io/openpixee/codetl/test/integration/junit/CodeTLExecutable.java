package io.openpixee.codetl.test.integration.junit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class CodeTLExecutable {

  private final Path binary;

  public CodeTLExecutable(final Path binary) {
    this.binary = Objects.requireNonNull(binary);
  }

  public void execute(final String... args) throws IOException, InterruptedException {
    final var process = newProcessBuilder(args);
    final var err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    final int code = process.waitFor();
    if (code != 0) {
      throw new CodeTLExecutionException(code, err);
    }
  }

  public Process newProcessBuilder(final String... args) throws IOException {
    final var command = new ArrayList<String>(args.length + 1);
    command.add(binary.toString());
    command.addAll(Arrays.asList(args));
    return new ProcessBuilder(command).start();
  }
}
