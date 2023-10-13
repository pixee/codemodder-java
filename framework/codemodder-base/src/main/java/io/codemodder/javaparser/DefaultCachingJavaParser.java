package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class DefaultCachingJavaParser implements CachingJavaParser {

  private final JavaParser parser;
  private final Map<Path, CompilationUnit> cache;

  DefaultCachingJavaParser(final JavaParser parser) {
    this.parser = Objects.requireNonNull(parser);
    this.cache = new HashMap<>();
  }

  @Override
  public CompilationUnit parseJavaFile(final Path file, final String contents) {
    if (cache.containsKey(file)) {
      return cache.get(file);
    }
    final ParseResult<CompilationUnit> result = parser.parse(contents);
    if (!result.isSuccessful()) {
      throw new RuntimeException("can't parse file");
    }
    CompilationUnit cu = result.getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    cache.put(file, cu);
    return cu;
  }
}
