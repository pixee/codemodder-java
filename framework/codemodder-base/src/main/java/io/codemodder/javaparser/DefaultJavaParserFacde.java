package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.nio.file.Path;
import java.util.Objects;

final class DefaultJavaParserFacde implements JavaParserFacde {

  private final JavaParser parser;

  DefaultJavaParserFacde(final JavaParser parser) {
    this.parser = Objects.requireNonNull(parser);
  }

  @Override
  public CompilationUnit parseJavaFile(final Path file, final String contents) {
    final ParseResult<CompilationUnit> result = parser.parse(contents);
    if (!result.isSuccessful()) {
      throw new RuntimeException("can't parse file");
    }
    CompilationUnit cu = result.getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    return cu;
  }
}
