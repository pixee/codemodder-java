package io.codemodder.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import java.util.List;

/** Uses JavaParser to change Java source files. */
public interface JavaParserChanger extends CodeChanger {

  /** Called when a Java file, which has already been parsed into a compilation unit, is seen. */
  List<CodemodChange> visit(final CodemodInvocationContext context, final CompilationUnit cu);

  /** Return the list of dependencies associated with the "fix" provided by this changer. */
  default List<DependencyGAV> dependenciesRequired() {
    return List.of();
  }
}
