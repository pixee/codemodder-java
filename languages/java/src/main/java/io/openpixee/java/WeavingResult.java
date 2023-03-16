package io.openpixee.java;

import io.codemodder.ChangedFile;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** Represents the technical results of a scan. */
public interface WeavingResult {

  /** A collection of the changes we want to make. */
  Set<ChangedFile> changedFiles();

  /** A collection of files that failed to be parsed. */
  Set<String> unscannableFiles();

  class Default implements WeavingResult {

    private final Set<ChangedFile> changedFiles;
    private final Set<String> unscannableFiles;

    Default(final Set<ChangedFile> changedFiles, final Set<String> unscannableFiles) {
      this.changedFiles =
          Collections.unmodifiableSet(Objects.requireNonNull(changedFiles, "changedFiles"));
      this.unscannableFiles =
          Collections.unmodifiableSet(Objects.requireNonNull(unscannableFiles, "unscannableFiles"));
    }

    @Override
    public Set<ChangedFile> changedFiles() {
      return changedFiles;
    }

    @Override
    public Set<String> unscannableFiles() {
      return unscannableFiles;
    }
  }

  /** A convenience method to return a weave that represents no results. */
  static WeavingResult empty() {
    return createDefault(Collections.emptySet(), Collections.emptySet());
  }

  static WeavingResult createDefault(
      final Set<ChangedFile> changedFiles, final Set<String> unscannableFiles) {
    return new Default(changedFiles, unscannableFiles);
  }
}
