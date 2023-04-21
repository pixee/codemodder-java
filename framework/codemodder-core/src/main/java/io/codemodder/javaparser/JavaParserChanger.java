package io.codemodder.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.Changer;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.DependencyGAV;
import java.util.List;

/** Uses JavaParser to change Java source files. */
public interface JavaParserChanger extends Changer {

  /** Called when a Java file, which has already been parsed into a compilation unit, is seen. */
  void visit(final CodemodInvocationContext context, final CompilationUnit cu);

  /** Return the list of dependencies associated with the "fix" provided by this changer. */
  default List<DependencyGAV> dependenciesRequired() {
    return List.of();
  }
}
