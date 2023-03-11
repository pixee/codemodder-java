package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import java.util.Optional;

/** {@inheritDoc} Uses JavaParser to change Java source files. */
public interface JavaParserChanger extends Changer {

  /**
   * Creates a visitor for a given Java source file, or not. It's up to the implementing type to
   * determine if and how source file should be changed.
   */
  Optional<ModifierVisitor<ChangeContext>> createModifierVisitor(CompilationUnit cu);
}
