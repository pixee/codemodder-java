package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.codetf.CodeTFChangesetEntry;
import java.util.List;
import java.util.Objects;

/** The result of updating a pom file. */
final class PomUpdateResult {

  private final List<CodeTFChangesetEntry> entry;
  private final List<DependencyGAV> skippedDependencies;

  PomUpdateResult(
      final List<CodeTFChangesetEntry> entry, final List<DependencyGAV> skippedDependencies) {
    this.entry = Objects.requireNonNull(entry);
    this.skippedDependencies = Objects.requireNonNull(skippedDependencies);
  }

  List<CodeTFChangesetEntry> getEntry() {
    return entry;
  }

  List<DependencyGAV> getSkippedDependencies() {
    return skippedDependencies;
  }
}
