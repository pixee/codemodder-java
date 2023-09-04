package io.codemodder.plugins.llm;

import io.codemodder.EncodingDetector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Provides some methods to simplify working with the files. */
final class FileDescription {

  private final String fileName;
  private final Charset charset;

  private final String lineSeparator;
  private final List<String> lines;

  FileDescription(final Path path) {
    fileName = path.getFileName().toString();

    try {
      charset = Charset.forName(EncodingDetector.create().detect(path).orElse("UTF-8"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try {
      String s = Files.readString(path, charset);
      lineSeparator = detectLineSeparator(s);
      lines = List.of(s.split("\\R", -1));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  String getFileName() {
    return fileName;
  }

  Charset getCharset() {
    return charset;
  }

  String getLineSeparator() {
    return lineSeparator;
  }

  List<String> getLines() {
    return lines;
  }

  String formatLinesWithLineNumbers() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      sb.append(i + 1);
      sb.append(": ");
      sb.append(lines.get(i));
      sb.append("\n");
    }
    return sb.toString();
  }

  private String detectLineSeparator(final String s) {
    Matcher m = Pattern.compile("(\\R)").matcher(s);
    if (m.find()) {
      // This assumes that the first line separator found is the one to use.
      return m.group(1);
    }
    return "\n";
  }
}
