package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.ASTExpectations.expect;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.inject.Inject;

/** Moves strings to {@link StandardCharsets} fields. */
@Codemod(
    id = "pixee:java/switch-to-standard-charsets",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SwitchToStandardCharsetsCodemod extends CompositeJavaParserChanger {

  @Inject
  public SwitchToStandardCharsetsCodemod(
      final GetBytesCodemod getBytesCodemod, final CharsetForNameCodemod charsetForNameCodemode) {
    super(getBytesCodemod, charsetForNameCodemode);
  }

  private static class GetBytesCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {
    private static final String RULE =
        """
                rules:
                  - id: switch-to-standard-charsets
                    patterns:
                      - pattern: (String $X).getBytes("...")
                """;

    @Inject
    private GetBytesCodemod(@SemgrepScan(yaml = RULE) final RuleSarif ruleSarif) {
      super(ruleSarif, MethodCallExpr.class, CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr methodCallExpr,
        final Result result) {

      final Optional<Charset> c = getSpecifiedCharset(methodCallExpr.getArgument(0));
      if (c.isEmpty()) {
        return false;
      }

      FieldAccessExpr field =
          new FieldAccessExpr(
              new NameExpr(StandardCharsets.class.getSimpleName()), getCharsetFieldName(c.get()));
      methodCallExpr.setArgument(0, field);
      addImportIfMissing(cu, StandardCharsets.class);

      Exceptions.cleanupExceptionHandling(
          field.getParentNode().get(), unsupportedEncodingExceptionFqcn);
      return true;
    }
  }

  private static class CharsetForNameCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {
    private static final String RULE =
        """
                rules:
                  - id: switch-to-standard-charsets-in-forname
                    patterns:
                      - pattern: Charset.forName("...")
                """;

    @Inject
    private CharsetForNameCodemod(@SemgrepScan(yaml = RULE) final RuleSarif ruleSarif) {
      super(ruleSarif, MethodCallExpr.class, CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr methodCallExpr,
        final Result result) {

      final Optional<Charset> c = getSpecifiedCharset(methodCallExpr.getArgument(0));
      if (c.isEmpty()) {
        return false;
      }

      FieldAccessExpr field =
          new FieldAccessExpr(
              new NameExpr(StandardCharsets.class.getSimpleName()), getCharsetFieldName(c.get()));
      Optional<Node> parentNode = methodCallExpr.getParentNode();
      parentNode.get().replace(methodCallExpr, field);
      addImportIfMissing(cu, StandardCharsets.class);
      Exceptions.cleanupExceptionHandling(parentNode.get(), unsupportedEncodingExceptionFqcn);
      return true;
    }
  }

  /**
   * Get the standard charset specified by the first argument to this method.
   *
   * @param node the string literal argument
   * @return a {@link Charset} if the argument is a known charset, otherwise {@link Optional#empty}
   */
  private static Optional<Charset> getSpecifiedCharset(final Node node) {
    Optional<StringLiteralExpr> charsetRef = expect(node).toBeStringLiteral().result();

    if (charsetRef.isEmpty()) {
      return Optional.empty();
    }

    StringLiteralExpr charsetExpr = charsetRef.get();
    String charset = charsetExpr.getValue();

    final Charset c;
    switch (charset) {
      case "US-ASCII" -> c = StandardCharsets.US_ASCII;
      case "UTF-8" -> c = StandardCharsets.UTF_8;
      case "UTF-16" -> c = StandardCharsets.UTF_16;
      case "UTF-16LE" -> c = StandardCharsets.UTF_16LE;
      case "UTF-16BE" -> c = StandardCharsets.UTF_16BE;
      case "ISO-8859-1" -> c = StandardCharsets.ISO_8859_1;
      default -> {
        return Optional.empty();
      }
    }
    return Optional.of(c);
  }

  private static String getCharsetFieldName(final Charset c) {
    return c.name().replace("-", "_");
  }

  private static final String unsupportedEncodingExceptionFqcn =
      "java.io.UnsupportedEncodingException";
}
