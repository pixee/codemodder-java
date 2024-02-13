package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.removeImportIfUnused;
import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Objects;
import javax.inject.Inject;

/**
 * Migrates {@link org.apache.commons.io.FileUtils} APIs to {@link java.nio.file.Files} where
 * possible. Some of the contracts between FileUtils and Files are similar, but not exactly the
 * same, so they're not good candidates for simple migration of APIs.
 */
@Codemod(
    id = "pixee:java/migrate-files-commons-io-to-nio",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class MigrateFilesCommonsIOToNIOCodemod extends CompositeJavaParserChanger {

  @Inject
  public MigrateFilesCommonsIOToNIOCodemod(
      final ReadLinesCodemod readLinesCodemod,
      final ReadStringCodemod readStringCodemod,
      final ReadBytesCodemod readBytesCodemod) {
    super(readLinesCodemod, readStringCodemod, readBytesCodemod);
  }

  private static class ReadLinesCodemod extends CommonsToNIOToPathChanger {
    private static final String RULE =
        """
            rules:
              - id: migrate-files-commons-io-to-nio-read-file-to-lines
                pattern-either:
                  - pattern: (org.apache.commons.io.FileUtils).readLines($X)
                  - pattern: (org.apache.commons.io.FileUtils).readLines($X, (Charset $Y))
            """;

    @Inject
    private ReadLinesCodemod(@SemgrepScan(yaml = RULE) final RuleSarif sarif) {
      super(sarif, "readAllLines");
    }
  }

  private static class ReadStringCodemod extends CommonsToNIOToPathChanger {
    private static final String RULE =
        """
            rules:
              - id: migrate-files-commons-io-to-nio-read-file-to-string
                pattern-either:
                  - pattern: (org.apache.commons.io.FileUtils).readFileToString($X)
                  - pattern: (org.apache.commons.io.FileUtils).readFileToString($X, (Charset $Y))
            """;

    @Inject
    private ReadStringCodemod(@SemgrepScan(yaml = RULE) final RuleSarif sarif) {
      super(sarif, "readString");
    }
  }

  private static class ReadBytesCodemod extends CommonsToNIOToPathChanger {
    private static final String RULE =
        """
            rules:
              - id: migrate-files-commons-io-to-nio-read-file-to-bytes
                pattern: (org.apache.commons.io.FileUtils).readFileToByteArray($X)
            """;

    @Inject
    private ReadBytesCodemod(@SemgrepScan(yaml = RULE) final RuleSarif sarif) {
      super(sarif, "readAllBytes");
    }
  }

  private abstract static class CommonsToNIOToPathChanger
      extends SarifPluginJavaParserChanger<MethodCallExpr> {
    private final String methodName;

    private CommonsToNIOToPathChanger(final RuleSarif ruleSarif, final String methodName) {
      super(ruleSarif, MethodCallExpr.class, CodemodReporterStrategy.empty());
      this.methodName = Objects.requireNonNull(methodName);
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr readFileToStringCall,
        final Result result) {
      switchFirstArgumentToPath(readFileToStringCall.getArguments());
      boolean success =
          replace(readFileToStringCall)
              .withStaticMethod("java.nio.file.Files", methodName)
              .withSameArguments();

      if (success) {
        removeImportIfUnused(cu, "org.apache.commons.io.FileUtils");
      }

      return success;
    }

    /**
     * Changes the first argument of the given {@link NodeList} to a {@link java.nio.file.Path}.
     * Assumes the first argument is a {@link java.io.File}.
     */
    private static void switchFirstArgumentToPath(final NodeList<Expression> arguments) {
      MethodCallExpr toPath = new MethodCallExpr(arguments.get(0), "toPath");
      arguments.set(0, toPath);
    }
  }
}
