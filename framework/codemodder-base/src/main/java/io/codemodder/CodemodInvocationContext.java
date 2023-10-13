package io.codemodder;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** The context we provide to each codemod. */
public interface CodemodInvocationContext {

  /** The includes/excludes for the file being changed. */
  LineIncludesExcludes lineIncludesExcludes();

  /** The root directory where the project being transformed lives. */
  CodeDirectory codeDirectory();

  /** The individual file being changed. */
  Path path();

  /** The ID of the codemod changing the file. */
  String codemodId();

  /** The original contents of the file before this codemod */
  String contents();

  /**
   * Convenience method for stream-wise processing lines of the file being changed with line
   * numbers.
   *
   * @return {@link Stream} of lines (with indices) of the file being changed. Must be closed.
   * @throws IOException when fails to read the file
   */
  default Stream<Line> lines() throws IOException {
    return Streams.mapWithIndex(
        Files.lines(path()), (content, line) -> new DefaultLine((int) line + 1, content));
  }
}
