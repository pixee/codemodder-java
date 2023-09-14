package io.codemodder;

import java.util.Optional;

/**
 * A dependency descriptor that renders a dependency as a markdown table, with a links to more info.
 */
final class MarkdownDependencyDescriptor implements DependencyDescriptor {

  @Override
  public String create(final DependencyGAV dependency) {
    StringBuilder sb = new StringBuilder();
    Optional<String> justification = dependency.justification();
    if (justification.isPresent()) {
      sb.append(justification.get());
    } else {
      sb.append("This dependency change is required to use the new code.");
    }
    sb.append("\n\n");

    boolean hadPreviousFooterItem = false;
    Optional<String> license = dependency.license();
    if (license.isPresent()) {
      hadPreviousFooterItem = true;
      sb.append("License: ");
      sb.append(license.get());
      if (DependencyLicenses.isOpenSource(license.get())) {
        sb.append(" ✅");
      } else {
        // we don't know, so use a question mark
        sb.append(" ❓");
      }
    }

    if (hadPreviousFooterItem) {
      sb.append(" | ");
    }

    Optional<String> repositoryUrl = dependency.repositoryUrl();
    if (repositoryUrl.isPresent()) {
      hadPreviousFooterItem = true;
      sb.append("[Open source](");
      sb.append(repositoryUrl.get());
      sb.append(") ✅");
    }

    Optional<Boolean> hasNoTransitiveDependencies = dependency.hasNoTransitiveDependencies();
    if (hasNoTransitiveDependencies.isPresent()) {
      if (hasNoTransitiveDependencies.get()) {
        if (hadPreviousFooterItem) {
          sb.append(" | ");
        } else {
          hadPreviousFooterItem = true;
        }
        sb.append("No transitive dependencies ");
        sb.append("✅");
      }
    }

    if (hadPreviousFooterItem) {
      sb.append(" | ");
    }

    String moreFactsUrl =
        String.format(
            "https://mvnrepository.com/artifact/%s/%s/%s",
            dependency.group(), dependency.artifact(), dependency.version());
    sb.append("[More facts](");
    sb.append(moreFactsUrl);
    sb.append(")\n");

    return sb.toString();
  }
}
