package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * {@inheritDoc}
 *
 * <p>This type specializes in non-Java file.
 */
public final class JavaParserCodemodRunner implements CodemodRunner {

  private final JavaParserChanger javaParserChanger;
  private final JavaParser parser;
  private final EncodingDetector encodingDetector;

  public JavaParserCodemodRunner(
      final JavaParser parser,
      final JavaParserChanger javaParserChanger,
      final EncodingDetector encodingDetector) {
    this.parser = Objects.requireNonNull(parser);
    this.javaParserChanger = Objects.requireNonNull(javaParserChanger);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
  }

  @Override
  public List<CodemodChange> run(final CodemodInvocationContext context) throws IOException {
    Path file = context.path();
    CompilationUnit cu = parseJavaFile(parser, file);
    List<CodemodChange> changes = javaParserChanger.visit(context, cu);
    if (!changes.isEmpty()) {
      String encodingName = encodingDetector.detect(file).orElse("UTF-8");
      Charset encoding = Charset.forName(encodingName);
      String modified = (LexicalPreservingPrinter.print(cu));
      Files.write(file, modified.getBytes(encoding), StandardOpenOption.TRUNCATE_EXISTING);
    }
    return changes;
  }

  private CompilationUnit parseJavaFile(final JavaParser javaParser, final Path file)
      throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      final ParseResult<CompilationUnit> result = javaParser.parse(in);
      if (!result.isSuccessful()) {
        throw new RuntimeException("can't parse file");
      }
      return result.getResult().orElseThrow();
    }
  }
}
