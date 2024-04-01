package io.codemodder.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

final class TestUtils {

  static CompilationUnit parseCode(final String code) {
    final ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    parserConfiguration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
    final var parser = new JavaParser(parserConfiguration);
    return parser.parse(code).getResult().get();
  }
}
