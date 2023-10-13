package io.codemodder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileCacheTest {

  private Path file1;
  private Path file2;
  private Path file3;
  private FileCache fileCache;

  @BeforeEach
  void setup(@TempDir final Path tmpDir) throws IOException {
    this.file1 = tmpDir.resolve("file1");
    this.file2 = tmpDir.resolve("file2");
    this.file3 = tmpDir.resolve("file3");

    Files.writeString(file1, "1");
    Files.writeString(file2, "2");
    Files.writeString(file3, "3");

    fileCache = FileCache.createDefault(2);
  }

  @Test
  void it_caches_correctly() throws IOException {
    String file1Contents = fileCache.get(file1);

    // not only should it be identical...
    assertThat(file1Contents).isEqualTo("1");

    // ... but it should also be the same pointer
    assertThat(file1Contents).isSameAs(file1Contents);
  }

  @Test
  void it_respects_max() throws IOException {
    String file1Contents = fileCache.get(file1);
    String file2Contents = fileCache.get(file2);

    assertThat(file1Contents).isSameAs(file1Contents);
    assertThat(file2Contents).isSameAs(file2Contents);

    String file3Contents = fileCache.get(file3);
    assertThat(file3Contents).isEqualTo(fileCache.get(file3));
    assertThat(file3Contents).isEqualTo("3");

    // confirm that the cache didn't retain
    String file3ContentsAgain = fileCache.get(file3);
    assertThat(file3Contents).isNotSameAs(file3ContentsAgain);
  }

  @Test
  void it_overrides() throws IOException {
    String file1Contents = fileCache.get(file1);
    assertThat(file1Contents).isEqualTo("1");
    fileCache.overrideEntry(file1, "skadoodle");
    assertThat(fileCache.get(file1)).isEqualTo("skadoodle");

    // we don't have an entry for file3, so attempting to override it should cause an error
    assertThrows(
        IllegalArgumentException.class, () -> fileCache.overrideEntry(file3, "irrelevant"));
  }
}
