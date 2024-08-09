package io.codemodder.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.*;
import java.io.IOException;
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
  private final JavaParserFacade parser;
  private final EncodingDetector encodingDetector;
  private final IncludesExcludes rootedFileMatcher;

  public JavaParserCodemodRunner(
      final JavaParserFacade parser,
      final JavaParserChanger javaParserChanger,
      final Path projectDir,
      final IncludesExcludes globalIncludesExcludes,
      final EncodingDetector encodingDetector) {
    this.parser = Objects.requireNonNull(parser);
    this.javaParserChanger = Objects.requireNonNull(javaParserChanger);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
    if (globalIncludesExcludes instanceof IncludesExcludes.MatchesEverything) {
      this.rootedFileMatcher =
          javaParserChanger.getIncludesExcludesPattern().getRootedMatcher(projectDir);
    } else {
      this.rootedFileMatcher = Objects.requireNonNull(globalIncludesExcludes);
    }
  }

  @Override
  public boolean supports(final Path path) {
    return path.getFileName().toString().endsWith(".java")
        && rootedFileMatcher.shouldInspect(path.toFile());
  }

  @Override
  public CodemodFileScanningResult run(final CodemodInvocationContext context) throws IOException {

    if (!javaParserChanger.shouldRun()) {
      return CodemodFileScanningResult.none();
    }
    Path file = context.path();
    CompilationUnit cu = parser.parseJavaFile(file);
    CodemodFileScanningResult result = javaParserChanger.visit(context, cu);
    List<CodemodChange> changes = result.changes();
    if (!changes.isEmpty()) {
      String encodingName = encodingDetector.detect(file).orElse("UTF-8");
      Charset encoding = Charset.forName(encodingName);
      String modified = (LexicalPreservingPrinter.print(cu));
      Files.writeString(file, modified, encoding, StandardOpenOption.TRUNCATE_EXISTING);
    }
    return result;
  }
}
