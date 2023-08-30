package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.codemodder.SourceDirectory;
import java.util.List;

final class DefaultJavaParserFactory implements JavaParserFactory {

  @Override
  public JavaParser create(final List<SourceDirectory> sourceDirectories) {
    final JavaParser javaParser = new JavaParser();
    final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ReflectionTypeSolver());
    sourceDirectories.forEach(
        javaDirectory -> combinedTypeSolver.add(new JavaParserTypeSolver(javaDirectory.path())));
    ParserConfiguration parserConfiguration = javaParser.getParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    return javaParser;
  }
}
