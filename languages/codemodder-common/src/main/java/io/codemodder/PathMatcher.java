package io.codemodder;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/** This type is used in include/exclude logic for matching paths. */
public final class PathMatcher {

  private final Integer line;
  private final String repositoryRootPath;
  private final java.nio.file.PathMatcher matcher;

  public PathMatcher(
      final FileSystem fs,
      final File repositoryRoot,
      final String pathPattern,
      final Integer line) {
    this.repositoryRootPath = Path.of(repositoryRoot.getAbsolutePath()).normalize().toString();
    Objects.requireNonNull(pathPattern);
    this.matcher = fs.getPathMatcher("glob:" + pathPattern);
    this.line = line;
  }

  /** Return if this path matcher matches the given file. */
  public boolean matches(final File file) {
    String candidateFilePath = Path.of(file.getAbsolutePath()).normalize().toString();
    String relativeCandidateFilePath = candidateFilePath.substring(repositoryRootPath.length());
    if (!relativeCandidateFilePath.startsWith("/")) {
      relativeCandidateFilePath = "/" + relativeCandidateFilePath;
    }
    return matcher.matches(Paths.get(relativeCandidateFilePath));
  }

  public Integer line() {
    return line;
  }

  public boolean targetsLine() {
    return line != null;
  }

  @Override
  public String toString() {
    return this.matcher.toString() + ":" + this.line;
  }
}
