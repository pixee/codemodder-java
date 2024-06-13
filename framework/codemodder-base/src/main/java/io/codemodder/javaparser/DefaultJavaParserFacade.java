package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Provider;

final class DefaultJavaParserFacade implements JavaParserFacade {

  private final Provider<JavaParser> parserProvider;
  private final ThreadLocal<JavaParser> javaParserRef;

  DefaultJavaParserFacade(final Provider<JavaParser> parserProvider) {
    this.parserProvider = Objects.requireNonNull(parserProvider);
    this.javaParserRef = new ThreadLocal<>();
  }

  @Override
  public CompilationUnit parseJavaFile(final Path file) throws IOException {
    JavaParser parser = javaParserRef.get();
    if (parser == null) {
      parser = parserProvider.get();
      javaParserRef.set(parser);
    }

    final ParseResult<CompilationUnit> result = parser.parse(file);
    if (!result.isSuccessful()) {
      throw new RuntimeException(
          "Error while parsing file " + file + " encountered problems: " + result.getProblems());
    }
    CompilationUnit cu = result.getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    return cu;
  }
}
