package io.codemodder.plugins.llm;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/** Provides a set of useful methods to simplify working with the files. */
public interface FileDescription {

  /** Returns the file name. */
  String getFileName();

  /** Returns the file's charset. Assumed to be UTF-8 if none can be detected. */
  Charset getCharset();

  /**
   * Returns the file's preferred line separator by locating the first line separator and assuming
   * "\n" if none are found.
   */
  String getLineSeparator();

  /** Returns the file as a list of lines. */
  List<String> getLines();

  /**
   * Return the file as a single string, but with each line prefixed with the line number, starting
   * with 1.
   */
  String formatLinesWithLineNumbers();

  static FileDescription from(final Path path) {
    return new DefaultFileDescription(path);
  }
}
