package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.FileWeavingContext;
import io.codemodder.JavaParserChanger;
import io.codemodder.RuleSarif;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Provides base functionality for making JavaParser-based changes with Semgrep. */
public abstract class SemgrepJavaParserChanger implements JavaParserChanger {

  protected final RuleSarif sarif;

  protected SemgrepJavaParserChanger(final RuleSarif semgrepSarif) {
    this.sarif = Objects.requireNonNull(semgrepSarif);
  }

  @Override
  public Optional<ModifierVisitor<FileWeavingContext>> createModifierVisitor(
      final CodemodInvocationContext context) {
    List<Region> regions = sarif.getRegionsFromResultsByRule(context.path());
    return !regions.isEmpty() ? Optional.of(createVisitor(context, regions)) : Optional.empty();
  }

  /**
   * Creates a visitor for the given context and locations.
   *
   * @param context the context of this files transformation
   * @param regions the places in this file that have been identified as needing change by the
   *     static analysis
   * @return a visitor that will perform the necessary changes in the given source code file
   *     positions
   */
  public abstract ModifierVisitor<FileWeavingContext> createVisitor(
      CodemodInvocationContext context, List<Region> regions);
}
