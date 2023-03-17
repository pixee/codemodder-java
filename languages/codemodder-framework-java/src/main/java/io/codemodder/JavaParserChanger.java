package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;

/** {@inheritDoc} Uses JavaParser to change Java source files. */
public interface JavaParserChanger extends Changer {

  /** */
  void visit(final CodemodInvocationContext context, final CompilationUnit cu);
}
