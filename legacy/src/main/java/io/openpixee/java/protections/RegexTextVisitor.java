package io.openpixee.java.protections;

import io.codemodder.ChangedFile;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.codemodder.WeavingResult;
import io.openpixee.java.FileBasedVisitor;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type does the heavy lifting for many protections that can work in a simple
 * "search-and-replace" pattern for non-Java code.
 */
public abstract class RegexTextVisitor implements FileBasedVisitor {

  private final Predicate<File> fileMatcher;
  private final Pattern pattern;
  private final String weaveCode;
  private final boolean removeEmptyLeftoverLines;
  private final WeavingResult emptyWeaveResult;
  private final DependencyGAV dependencyNeeded;

  public RegexTextVisitor(
      final Predicate<File> fileMatcher,
      final Pattern pattern,
      final String weaveCode,
      final DependencyGAV dependencyNeeded) {
    this(fileMatcher, pattern, weaveCode, dependencyNeeded, false);
  }

  public RegexTextVisitor(
      final Predicate<File> fileMatcher,
      final Pattern pattern,
      final String weaveCode,
      final DependencyGAV dependencyNeeded,
      final boolean removeEmptyLeftoverLines) {
    this.fileMatcher = Objects.requireNonNull(fileMatcher, "fileMatcher");
    this.pattern = Objects.requireNonNull(pattern, "pattern");
    this.weaveCode = Objects.requireNonNull(weaveCode, "weaveCode");
    this.dependencyNeeded = dependencyNeeded;
    this.removeEmptyLeftoverLines = removeEmptyLeftoverLines;
    this.emptyWeaveResult =
        WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final FileWeavingContext weavingContext,
      final Set<ChangedFile> changedJavaFiles) {

    if (fileMatcher.test(file)) {
      try {
        final ChangedFile changedFile = attemptWeave(file, weavingContext);
        if (changedFile != null) {
          var changedFiles = new HashSet<ChangedFile>();
          changedFiles.add(changedFile);
          return WeavingResult.createDefault(changedFiles, Collections.emptySet());
        }
      } catch (IOException e) {
        LOG.error("Problem analyzing file", e);
        return WeavingResult.createDefault(Collections.emptySet(), Set.of(file.getAbsolutePath()));
      }
    }
    return emptyWeaveResult;
  }

  private ChangedFile attemptWeave(final File file, final FileWeavingContext context)
      throws IOException {
    final List<Weave> weaves = new ArrayList<>();
    final String fileContents = FileUtils.readFileToString(file);
    final Matcher matcher = pattern.matcher(fileContents);
    StringBuilder rebuiltContents = null;
    int lastEnd = 0;
    final List<Integer> possibleEmptylines = new ArrayList<>();
    while (matcher.find()) {
      int start = matcher.start();
      int startLine = LineNumbers.getLineNumberAt(fileContents, start);
      if (!context.isLineIncluded(startLine)) {
        continue;
      }
      if (rebuiltContents == null) {
        rebuiltContents = new StringBuilder(fileContents.length() + 128);
      }
      int end = matcher.end();
      String snippet = fileContents.substring(start, end);
      rebuiltContents.append(fileContents, lastEnd, start);
      weaves.add(Weave.from(startLine, weaveCode, dependencyNeeded));

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
      return null;
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

    final File modifiedFile =
        File.createTempFile(file.getName(), getExtension(file.getName()).orElse(".tmp"));
    FileUtils.write(modifiedFile, rebuiltContents.toString());
    return ChangedFile.createDefault(file.getPath(), modifiedFile.getPath(), weaves);
  }

  private Optional<String> getExtension(final String name) {
    final int dotIndex = name.indexOf('.');
    if (dotIndex != name.length() - 1) {
      return Optional.of(name.substring(dotIndex));
    }
    return Optional.of(name);
  }

  /**
   * Given a snippet that matches the regex, return the replacement string. Some weavers will just
   * delete the snippet, others will wrap it in something, etc.
   */
  @NotNull
  public abstract String getReplacementFor(String matchingSnippet);

  private static final String nl = System.getProperty("line.separator");
  private static final Logger LOG = LoggerFactory.getLogger(RegexTextVisitor.class);
}
