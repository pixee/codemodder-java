package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import java.util.function.BiFunction;

/** Builds a remediation strategy from a function */
public final class ModularRemediationStrategy implements RemediationStrategy {

  private final BiFunction<CompilationUnit, Node, SuccessOrReason> fixer;

  public ModularRemediationStrategy(
      final BiFunction<CompilationUnit, Node, SuccessOrReason> fixer) {
    this.fixer = fixer;
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    return fixer.apply(cu, node);
  }
}
