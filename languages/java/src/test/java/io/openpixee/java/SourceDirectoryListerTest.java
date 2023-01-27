package io.openpixee.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SourceDirectoryListerTest {

  @Test
  void it_finds_java_dirs(@TempDir final Path tmp) throws IOException {
    SourceDirectoryLister lister = SourceDirectoryLister.createDefault();
    var dir = tmp.resolve("src/main/java/com/acme");
    Files.createDirectories(dir);
    Files.createFile(dir.resolve("Example.java"));
    List<SourceDirectory> sourceDirs = lister.listJavaSourceDirectories(List.of(tmp.toFile()));
    assertThat(sourceDirs, hasSize(1));
    assertThat(sourceDirs.get(0).path(), endsWith("src/main/java"));
    assertThat(sourceDirs.get(0).files(), hasSize(1));

    // assert that we find a real java file
    assertThat(sourceDirs.get(0).files().get(0), endsWith("Example.java"));

    // assert that we don't find a test java file
    assertThat(
        sourceDirs.get(0).files().stream().anyMatch(f -> f.endsWith("JavaFixitCliRunTest.java")),
        equalTo(false));
  }
}
