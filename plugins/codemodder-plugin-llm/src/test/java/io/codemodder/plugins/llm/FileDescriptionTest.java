package io.codemodder.plugins.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FileDescriptionTest {

  @Test
  void it_returns_correct_file_info() throws IOException {
    Path tmp = Files.createTempFile("test", ".txt");
    Files.writeString(tmp, "a\nb\nc\n", StandardCharsets.UTF_8);
    FileDescription fd = FileDescription.from(tmp);
    assertThat(fd.getFileName()).isEqualTo(tmp.getFileName().toString());

    /*
     * There are no UTF-8 chars or a BOM, so it appears to our library as US-ASCII. This feels fine for now, but not great. Not sure what to do about it.
     */
    assertThat(fd.getCharset()).isEqualTo(StandardCharsets.US_ASCII);

    assertThat(fd.getLineSeparator()).isEqualTo("\n");
    assertThat(fd.getLines()).containsExactly("a", "b", "c", "");
    assertThat(fd.formatLinesWithLineNumbers()).isEqualTo("1: a\n2: b\n3: c\n4: \n");
  }

  @Test
  void it_handles_utf8_and_cr() throws IOException {
    Path tmp = Files.createTempFile("utf-8", ".txt");
    Files.writeString(tmp, "🧚\r\ntest", StandardCharsets.UTF_8);
    FileDescription fd = FileDescription.from(tmp);
    assertThat(fd.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(fd.getLineSeparator()).isEqualTo("\r\n");
  }
}
