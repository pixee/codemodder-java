package io.openpixee.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.github.pixee.codetf.CodeTFFileExtensionScanned;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CodeTFReportGeneratorTest {

  @Test
  void it_counts_file_extensions_correctly() throws IOException {
    CodeTFReportGenerator.Default reportGenerator = new CodeTFReportGenerator.Default();
    List<CodeTFFileExtensionScanned> filesScanned =
        reportGenerator.getFilesScanned(new File("src/test/resources/ext_count"));
    assertThat(filesScanned.size(), equalTo(5));
    assertThat(
        filesScanned.stream()
            .anyMatch(ext -> "java".equals(ext.getExtension()) && 3 == ext.getCount()),
        is(true));
    assertThat(
        filesScanned.stream()
            .anyMatch(ext -> "jsp".equals(ext.getExtension()) && 1 == ext.getCount()),
        is(true));
    assertThat(
        filesScanned.stream()
            .anyMatch(ext -> "xml".equals(ext.getExtension()) && 1 == ext.getCount()),
        is(true));
    assertThat(
        filesScanned.stream()
            .anyMatch(ext -> "js".equals(ext.getExtension()) && 2 == ext.getCount()),
        is(true));
    assertThat(
        filesScanned.stream()
            .anyMatch(ext -> "txt".equals(ext.getExtension()) && 1 == ext.getCount()),
        is(true));
  }
}
