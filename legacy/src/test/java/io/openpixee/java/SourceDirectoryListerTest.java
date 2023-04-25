package io.openpixee.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import io.codemodder.SourceDirectory;
import io.codemodder.SourceDirectoryLister;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SourceDirectoryListerTest {

  @Test
  void it_finds_java_dirs() throws IOException {
    SourceDirectoryLister lister = SourceDirectoryLister.createDefault();
    List<SourceDirectory> sourceDirs = lister.listJavaSourceDirectories(List.of(new File(".")));
    sourceDirs.removeIf(dir -> dir.path().contains("spotlessJava"));
    assertThat(sourceDirs.size(), equalTo(1));
    assertThat(sourceDirs.get(0).path(), endsWith("src/main/java"));
    assertThat(sourceDirs.get(0).files().size(), greaterThan(33));

    // assert that we find a real java file
    assertThat(
        sourceDirs.get(0).files().stream().anyMatch(f -> f.endsWith("TypeLocator.java")),
        equalTo(true));

    // assert that we don't find a test java file
    assertThat(
        sourceDirs.get(0).files().stream().anyMatch(f -> f.endsWith("JavaFixitCliRunTest.java")),
        equalTo(false));
  }
}
