package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Removes calls to {@link org.apache.commons.collections4.CollectionUtils#isEmpty(Collection)}. */
@Codemod(
    id = "pixee:java/remove-collectionutils-isempty",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class RemoveCollectionUtilsIsEmptyCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public RemoveCollectionUtilsIsEmptyCodemod(
      @SemgrepScan(ruleId = "remove-collectionutils-isempty") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    Optional<Node> parentNode = methodCallExpr.getParentNode();
    if (parentNode.isEmpty()) {
      return false;
    }
    Expression argument = methodCallExpr.getArgument(0);
    final String name;
    if (argument.isNameExpr()) {
      name = argument.asNameExpr().getNameAsString();
    } else if (argument.isFieldAccessExpr()) {
      name = argument.asFieldAccessExpr().toString();
    } else {
      return false;
    }
    String newCode = String.format("%s == null || %s.isEmpty()", name, name);
    Expression newExpression = StaticJavaParser.parseExpression(newCode);
    parentNode.get().replace(methodCallExpr, newExpression);

    Path projectDir = context.codeDirectory().asPath();
    try {
      if (!hasReferencesToTypeBesidesThis(
          projectDir,
          "org.apache.commons.collections4",
          List.of(".java", ".kts", ".jsp", ".jspx"),
          List.of(context.path()))) {
        // TODO: use pom operator to remove dependency
      }
    } catch (IOException e) {
      LOG.warn(
          "Couldn't inspect files to determine if we could remove commons-collections4 dependency",
          e);
    }

    return true;
  }

  private boolean hasReferencesToTypeBesidesThis(
      final Path projectDir,
      final String token,
      final List<String> fileExtensions,
      final List<Path> exceptions)
      throws IOException {
    try (Stream<Path> stream = Files.walk(projectDir)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> fileExtensions.stream().anyMatch(path.toString().toLowerCase()::endsWith))
          .filter(path -> !exceptions.contains(path))
          .anyMatch(path -> fileContainsString(path, token));
    }
  }

  private boolean fileContainsString(final Path path, final String token) {
    try {
      return Files.readString(path).contains(token);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(RemoveCollectionUtilsIsEmptyCodemod.class);
}
