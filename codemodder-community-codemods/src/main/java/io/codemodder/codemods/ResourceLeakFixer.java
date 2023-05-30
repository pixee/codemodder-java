package io.codemodder.codemods;

import com.github.javaparser.ast.expr.*;
import io.codemodder.ast.*;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A library that contains methods for automatically fixing resource leaks detected by CodeQL's
 * rules "java/database-resource-leak", java/input-resource-leak, and java/output-resource-leak
 * whenever possible.
 */
final class ResourceLeakFixer {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceLeakFixer.class);

  /**
   * Detects if an {@link Expression} that creates a resource type is fixable and tries to fix it.
   * Combines {@code isFixable} and {@code tryToFix}.
   */
  public static Optional<Integer> checkAndFix(final Expression expr) {
    return Optional.empty();
  }
}
