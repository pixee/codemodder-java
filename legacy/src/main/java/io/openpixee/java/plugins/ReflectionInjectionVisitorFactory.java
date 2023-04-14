package io.openpixee.java.plugins;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.codemodder.ast.ASTTransforms;
import io.github.pixee.security.Reflection;
import io.openpixee.java.DoNothingVisitor;
import io.openpixee.java.Sarif;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protects against "reflection injection" weaknesses -- where user input is used to build a class
 * name that is sent into {@link Class#forName(String)}. This is probably overkill and possibly
 * dangerous if we do it for all calls in all code everywhere. Therefore, we limit this code to just
 * those places where an AST tool has decided there's an untrusted dataflow to the call.
 */
public final class ReflectionInjectionVisitorFactory implements VisitorFactory {

  private final File repositoryRoot;
  private final Set<Result> results;
  private final List<PhysicalLocation> locations;

  public ReflectionInjectionVisitorFactory(final File repositoryRoot, final Set<Result> results) {
    this.results = Objects.requireNonNull(results);
    this.repositoryRoot = repositoryRoot;
    this.locations =
        results.stream()
            .map(result -> result.getLocations().get(0).getPhysicalLocation())
            .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    for (PhysicalLocation location : locations) {
      try {
        if (Sarif.looksTheSame(location, repositoryRoot, file)) {
          return new ReflectionInjectionVisitor(
              cu, Sarif.getAllResultsByFile(results, location.getArtifactLocation().getUri()));
        }
      } catch (IOException e) {
        LOG.error("Problem assessing if rule matches file: {}", file, e);
      }
    }
    return new DoNothingVisitor();
  }

  @Override
  public String ruleId() {
    return ID;
  }

  private static class ReflectionInjectionVisitor extends ModifierVisitor<FileWeavingContext> {

    private final NameExpr callbackClass;
    private final Set<Result> results;
    private final CompilationUnit cu;

    private ReflectionInjectionVisitor(final CompilationUnit cu, final Set<Result> results) {
      this.cu = cu;
      this.callbackClass = new NameExpr(Reflection.class.getSimpleName());
      this.results = Objects.requireNonNull(results);
    }

    @Override
    public Visitable visit(MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      String methodName = methodCallExpr.getNameAsString();
      if ("forName".equals(methodName) && methodCallExpr.getArguments().size() == 1) {
        Expression expression = methodCallExpr.getArguments().get(0);
        if (!expression.isStringLiteralExpr()) {
          Optional<Expression> scopeRef = methodCallExpr.getScope();
          if (scopeRef.isPresent()) {
            Expression scope = scopeRef.get();
            Optional<Range> scopeRange = scope.getRange();
            if (scope.isNameExpr() && "Class".equals(scope.asNameExpr().getNameAsString())) {
              Range methodCallRange = methodCallExpr.getRange().get();
              int startLine = methodCallRange.begin.line;
              Optional<Result> resultRef =
                  Sarif.getFirstMatchingResult(
                      results, startLine, methodCallExpr.getNameAsString());
              if (resultRef.isPresent()
                  && context.isLineIncluded(methodCallExpr.getRange().get().begin.line)) {
                ASTTransforms.addImportIfMissing(cu, Reflection.class);
                MethodCallExpr safeCall = new MethodCallExpr();
                safeCall.setName("loadAndVerify");
                safeCall.setScope(callbackClass);
                safeCall.setArguments(methodCallExpr.getArguments());
                context.addWeave(
                    Weave.from(
                        scopeRange.get().begin.line,
                        ID,
                        List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT)));
                return super.visit(safeCall, context);
              }
            }
          }
        }
      }
      return super.visit(methodCallExpr, context);
    }
  }

  public static final String ID = "pixee:java/reflection-injection";
  private static final Logger LOG =
      LoggerFactory.getLogger(ReflectionInjectionVisitorFactory.class);
}
