package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DefaultCodeDirectoryTest {

  private DefaultCodeDirectory codeDirectory;
  private Path repoDir;

  @BeforeEach
  void setup(@TempDir final Path repoDir) throws IOException {
    Path testFile1 = repoDir.resolve("my/other/test/file1.java");
    Path srcFile1 = repoDir.resolve("src/main/file1.java");
    Path srcFile2 = repoDir.resolve("src/main/file2.java");

    Files.createDirectories(testFile1.getParent());
    Files.createFile(testFile1);

    Files.createDirectories(srcFile1.getParent());
    Files.createFile(srcFile1);

    Files.createDirectories(srcFile2.getParent());
    Files.createFile(srcFile2);

    Files.writeString(testFile1, "test file 1");
    Files.writeString(srcFile1, "src file 1");
    Files.writeString(srcFile2, "src file 2");

    this.repoDir = repoDir;
    codeDirectory = new DefaultCodeDirectory(repoDir);
  }

  @ParameterizedTest
  @MethodSource("fileTests")
  void it_finds_files(final String givenPath, final String expectedPath) throws IOException {
    Optional<Path> filesWithTrailingPath = codeDirectory.findFilesWithTrailingPath(givenPath);

    if (expectedPath == null) {
      assertThat(filesWithTrailingPath).isEmpty();
    } else {
      assertThat(filesWithTrailingPath).isPresent();
      Path expected = repoDir.resolve(expectedPath);
      assertThat(filesWithTrailingPath).contains(expected);
    }
  }

  private static Stream<Arguments> fileTests() {
    return Stream.of(
        Arguments.of("file1.java", "my/other/test/file1.java"),
        Arguments.of("main/file1.java", "src/main/file1.java"),
        Arguments.of("main//file1.java", "src/main/file1.java"),
        Arguments.of("main\\file1.java", "src/main/file1.java"),
        Arguments.of("src\\\\main\\file1.java", "src/main/file1.java"),
        Arguments.of("file2.java", "src/main/file2.java"),
        Arguments.of("file3.java", null));
  }
}
