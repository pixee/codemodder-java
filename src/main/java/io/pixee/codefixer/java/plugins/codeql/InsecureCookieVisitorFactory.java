package io.pixee.codefixer.java.plugins.codeql;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.DoNothingVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.Sarif;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.codefixer.java.protections.ASTs;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Fixes issues reported under the id "java/insecure-cookie" */
final class InsecureCookieVisitorFactory implements VisitorFactory {

  /** The locations of each result. */
  private final List<PhysicalLocation> locations;

  private final File repositoryRoot;

  InsecureCookieVisitorFactory(final File repositoryRoot, final Set<Result> results) {
    Objects.requireNonNull(results, "results");
    this.locations =
        results.stream()
            .map(result -> result.getLocations().get(0).getPhysicalLocation())
            .collect(Collectors.toUnmodifiableList());
    this.repositoryRoot = Objects.requireNonNull(repositoryRoot);
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    for (PhysicalLocation location : locations) {
      try {
        if (Sarif.looksTheSame(location, repositoryRoot, file)) {
          return new CookieSetSecureModifierVisitor(getAllLocationsWithSameFile(location));
        }
      } catch (IOException e) {
        LOG.error("Problem assessing if rule matches file: {}", file, e);
      }
    }
    return new DoNothingVisitor();
  }

    @Override
    public String ruleId() {
        return insecureCookieRuleId;
    }

    private List<PhysicalLocation> getAllLocationsWithSameFile(final PhysicalLocation location) {
    return locations.stream()
        .filter(
            loc ->
                loc.getArtifactLocation().getUri().equals(location.getArtifactLocation().getUri()))
        .collect(Collectors.toUnmodifiableList());
  }

  /** Inserts a call to cookie.setSecure(true) before calls to response.addCookie(cookie) */
  private static class CookieSetSecureModifierVisitor extends ModifierVisitor<FileWeavingContext> {
    private final List<PhysicalLocation> locations;

    private CookieSetSecureModifierVisitor(final List<PhysicalLocation> locations) {
      this.locations = Objects.requireNonNull(locations);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if ("addCookie".equals(methodCallExpr.getNameAsString())
          && methodCallExpr.getArguments().size() > 0) {
        Optional<Range> expRange = methodCallExpr.getRange();
        if (expRange.isPresent() && context.isLineIncluded(methodCallExpr)) {
          boolean instrumentedThisMethodCall = false;
          for (int i = 0; i < locations.size() && !instrumentedThisMethodCall; i++) {
            PhysicalLocation location = locations.get(i);
            Region findingRegion = location.getRegion();
            int startLine = expRange.get().begin.line;
            Integer resultStartLine = findingRegion.getStartLine();
            if (startLine == resultStartLine) {
              Optional<Statement> parentStatementRef = ASTs.findParentStatementFrom(methodCallExpr);
              if (parentStatementRef.isPresent() && methodCallExpr.getScope().isPresent()) {
                Expression cookieExpression = methodCallExpr.getArgument(0);
                Statement parentStatement = parentStatementRef.get();
                if (!cookieExpression.isMethodCallExpr()) {
                  ExpressionStmt newStatement = new ExpressionStmt();
                  MethodCallExpr secureCookieExpr =
                      new MethodCallExpr("setSecure", new BooleanLiteralExpr(true));
                  secureCookieExpr.setScope(cookieExpression);
                  newStatement.setExpression(secureCookieExpr);
                  ASTs.addStatementBeforeStatement(parentStatement, newStatement);
                  context.addWeave(Weave.from(startLine, insecureCookieRuleId));
                  instrumentedThisMethodCall = true;
                }
              }
            }
          }
        }
      }
      return super.visit(methodCallExpr, context);
    }
  }

  private static final String insecureCookieRuleId = "codeql:java/insecure-cookie";
  private static final Logger LOG = LogManager.getLogger(InsecureCookieVisitorFactory.class);
}
