package io.codemodder.codemods;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashSet;
import java.util.Set;

/**
 * The VariableCollector class is a visitor that traverses a {@link CompilationUnit} using the
 * {@link VoidVisitorAdapter}. It collects variable names from VariableDeclarator nodes and stores
 * them in a Set. The collected variables can be retrieved using the getDeclaredVariables method.
 */
final class VariableCollector extends VoidVisitorAdapter<Void> {
  private final Set<String> declaredVariables = new HashSet<>();

  /** Gets the set of declared variables collected by the VariableCollector. */
  Set<String> getDeclaredVariables() {
    return declaredVariables;
  }

  /** Overrides the visit method to collect variable names from VariableDeclarator nodes. */
  @Override
  public void visit(final VariableDeclarator declarator, final Void arg) {
    declaredVariables.add(declarator.getNameAsString());
    super.visit(declarator, arg);
  }
}
