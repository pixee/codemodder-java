package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

final class DefaultJavaParserFacade implements JavaParserFacade {

  private final JavaParser parser;

  DefaultJavaParserFacade(final JavaParser parser) {
    this.parser = Objects.requireNonNull(parser);
  }

  @Override
  public CompilationUnit parseJavaFile(final Path file) throws IOException {
    final ParseResult<CompilationUnit> result = parser.parse(file);
    if (!result.isSuccessful()) {
      throw new RuntimeException("can't parse file");
    }
    CompilationUnit cu = result.getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    return cu;
  }
}
