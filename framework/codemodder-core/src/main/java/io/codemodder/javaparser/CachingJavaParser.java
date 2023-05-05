package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsible for parsing Java files and maintaining the compilation units across different
 * accesses.
 */
public interface CachingJavaParser {

  /**
   * Return the {@link CompilationUnit} for the given Java file. If the given file has not been seen
   * before, it will be cached and all future invocations will return the same {@link
   * CompilationUnit}.
   *
   * @param file a Java file path
   * @return a {@link CompilationUnit} for the given file
   * @throws IOException if there is a file I/O error
   */
  CompilationUnit parseJavaFile(Path file) throws IOException;

  /** Return a simple implementation of the {@link CachingJavaParser} interface. */
  static CachingJavaParser from(final JavaParser parser) {
    return new DefaultCachingJavaParser(parser);
  }
}
