package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.codetf.CodeTFChangesetEntry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The result of updating a pom file. */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class PomUpdateResult {

  private final Optional<CodeTFChangesetEntry> entry;
  private final List<DependencyGAV> skippedDependencies;

  PomUpdateResult(
      final Optional<CodeTFChangesetEntry> entry, final List<DependencyGAV> skippedDependencies) {
    this.entry = Objects.requireNonNull(entry);
    this.skippedDependencies = Objects.requireNonNull(skippedDependencies);
  }

  Optional<CodeTFChangesetEntry> getEntry() {
    return entry;
  }

  List<DependencyGAV> getSkippedDependencies() {
    return skippedDependencies;
  }
}
