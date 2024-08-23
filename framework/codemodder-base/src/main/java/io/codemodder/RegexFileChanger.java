package io.codemodder;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * This type does the heavy lifting for many protections that can work in a simple
 * "search-and-replace" pattern for non-Java code.
 */
public abstract class RegexFileChanger extends RawFileChanger {

  private final Pattern pattern;
  private final boolean removeEmptyLeftoverLines;
  private final List<DependencyGAV> dependenciesRequired;

  protected RegexFileChanger(
      final Pattern pattern,
      final boolean removeEmptyLeftoverLines,
      final List<DependencyGAV> dependenciesRequired) {
    this.pattern = Objects.requireNonNull(pattern, "pattern");
    this.removeEmptyLeftoverLines = removeEmptyLeftoverLines;
    this.dependenciesRequired = dependenciesRequired;
  }

  protected RegexFileChanger(
      final Pattern pattern,
      final boolean removeEmptyLeftoverLines,
      final List<DependencyGAV> dependenciesRequired,
      final CodemodReporterStrategy reporter) {
    super(reporter);
    this.pattern = Objects.requireNonNull(pattern, "pattern");
    this.removeEmptyLeftoverLines = removeEmptyLeftoverLines;
    this.dependenciesRequired = dependenciesRequired;
  }

  @Override
  public CodemodFileScanningResult visitFile(final CodemodInvocationContext context)
      throws IOException {
    final List<CodemodChange> changes = new ArrayList<>();
    final String fileContents = Files.readString(context.path());
    final Matcher matcher = pattern.matcher(fileContents);
    StringBuilder rebuiltContents = null;
    int lastEnd = 0;
    final List<Integer> possibleEmptylines = new ArrayList<>();
    while (matcher.find()) {
      int start = matcher.start();
      int startLine = LineNumbers.getLineNumberAt(fileContents, start);
      if (!context.lineIncludesExcludes().matches(startLine)) {
        continue;
      }
      if (rebuiltContents == null) {
        rebuiltContents = new StringBuilder(fileContents.length() + 128);
      }
      int end = matcher.end();
      String snippet = fileContents.substring(start, end);
      rebuiltContents.append(fileContents, lastEnd, start);
      changes.add(CodemodChange.from(startLine, dependenciesRequired));

      final String replacement = getReplacementFor(snippet);
      rebuiltContents.append(replacement);
      lastEnd = end;

      if (removeEmptyLeftoverLines && replacement.isBlank()) {
        int lineOfSnippetEnd =
            LineNumbers.getLineNumberAt(rebuiltContents, rebuiltContents.length() - 1);
        possibleEmptylines.add(lineOfSnippetEnd);
      }
    }
    if (lastEnd == 0) {
      return CodemodFileScanningResult.withOnlyChanges(changes);
    }

    rebuiltContents.append(fileContents.substring(lastEnd));

    /*
     * If we have possible empty lines to remove, we have to rebuild rebuiltContents. we could have
     * done this inline but it would have been more complicated code to have to peek-ahead, read-behind
     * and put a bunch of arithmetic in.
     */
    if (removeEmptyLeftoverLines && !possibleEmptylines.isEmpty()) {
      var lineNumberReader = new LineNumberReader(new StringReader(rebuiltContents.toString()));
      rebuiltContents.setLength(0);
      String buff;
      while ((buff = lineNumberReader.readLine()) != null) {
        int lineNumber = lineNumberReader.getLineNumber();
        if (possibleEmptylines.contains(lineNumber)) {
          if (!StringUtils.isWhitespace(buff)) {
            rebuiltContents.append(buff);
            rebuiltContents.append(nl);
          }
        } else {
          rebuiltContents.append(buff);
          rebuiltContents.append(nl);
        }
      }
      rebuiltContents.delete(rebuiltContents.length() - nl.length(), rebuiltContents.length());
    }

    if (changes.isEmpty()) {
      return CodemodFileScanningResult.withOnlyChanges(changes);
    }

    Files.write(context.path(), rebuiltContents.toString().getBytes());
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }

  /**
   * Given a snippet that matches the regex, return the replacement string. Some weavers will just
   * delete the snippet, others will wrap it in something, etc.
   */
  public abstract String getReplacementFor(String matchingSnippet);

  private static final String nl = System.getProperty("line.separator");
}
