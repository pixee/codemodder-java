package io.openpixee.java.plugins.contrast;

import com.contrastsecurity.sarif.CodeFlow;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlow;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.codemodder.ast.ASTTransforms;
import io.openpixee.java.DoNothingVisitor;
import io.openpixee.java.Sarif;
import io.openpixee.java.TypeLocator;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempts to fix XSS vulnerabilities discovered by Contrast in Java code. Contrast discovers XSS
 * in many forms, so it's not possible to fix them all.
 */
final class JavaXssVisitorFactory implements VisitorFactory {

  private final Set<Result> results;
  private final List<PhysicalLocation> locations;
  private final File repositoryRoot;
  private final String ruleId; // could be "stored-xss" or "reflected-xss"

  JavaXssVisitorFactory(final File repositoryRoot, final Set<Result> results, final String ruleId) {
    this.results = Objects.requireNonNull(results);
    this.repositoryRoot = repositoryRoot;
    this.ruleId = Objects.requireNonNull(ruleId);
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
          return new JavaXssVisitor(
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
    return ruleId;
  }

  /**
   * Limit the changes to the following conditions:
   *
   * <ul>
   *   <li>There is only one method call expression on the given line
   *   <li>There is only one argument to the given method call
   *   <li>The type of the argument is a String
   * </ul>
   *
   * There is an opportunity here to get much smarter about the APIs that it reports, but we'll have
   * to do it carefully, sink-by-sink. Some considerations that will have to go into every API:
   * which argument is the vulnerable one? What data type is it, what if it's a byte[]?
   */
  private static class JavaXssVisitor extends ModifierVisitor<FileWeavingContext> {

    private final Set<Result> results;
    private final CompilationUnit cu;

    private JavaXssVisitor(final CompilationUnit cu, final Set<Result> results) {
      this.results = results;
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if (httpResponseWriteNames.contains(methodCallExpr.getNameAsString())) {
        Optional<Range> expRange = methodCallExpr.getRange();
        if (expRange.isPresent()
            && context.isLineIncluded(methodCallExpr.getRange().get().begin.line)) {
          int startLine = expRange.get().begin.line;
          Optional<Result> resultRef =
              Sarif.getFirstMatchingResult(results, startLine, methodCallExpr.getNameAsString());
          if (resultRef.isPresent()) {
            Result result = resultRef.get();
            if (methodCallExpr.getArguments().size() == 1) {
              TypeLocator locator = TypeLocator.createDefault(cu);
              Expression argument = methodCallExpr.getArgument(0);
              String type = locator.locateType(argument);
              if ("String".equals(type) || "java.lang.String".equals(type)) {
                ASTTransforms.addImportIfMissing(cu, "org.owasp.encoder.Encode");
                MethodCallExpr safeExpression =
                    new MethodCallExpr(new NameExpr("Encode"), "forHtml");
                safeExpression.setArguments(NodeList.nodeList(argument));
                methodCallExpr.setArguments(NodeList.nodeList(safeExpression));
                context.addWeave(
                    Weave.from(startLine, toWeaveId(result), DependencyGAV.OWASP_XSS_JAVA_ENCODER));
              } else {
                LOG.debug(
                    "Ignoring XSS result due to parameter not being a String: {}",
                    result.getCorrelationGuid());
              }
            } else {
              LOG.debug(
                  "Ignoring XSS result due to parameter not being a String: {}",
                  result.getCorrelationGuid());
            }
          }
        }
      }
      return super.visit(methodCallExpr, context);
    }

    private String toWeaveId(final Result result) {
      return ContrastScanPlugin.ruleBase + result.getRuleId();
    }

    private static final Set<String> httpResponseWriteNames =
        Set.of("print", "println", "printf", "format", "write");
  }

  private static Set<Integer> getFirst(final Set<Result> results) {
    Set<Integer> lineNumbers = new HashSet<>();
    for (Result result : results) {
      List<CodeFlow> codeFlows = result.getCodeFlows();
      CodeFlow codeFlow = codeFlows.get(0);
      List<ThreadFlow> threadFlows = codeFlow.getThreadFlows();
      ThreadFlow threadFlow = threadFlows.get(0);
      List<ThreadFlowLocation> locations = threadFlow.getLocations();
      ThreadFlowLocation lastThreadFlowLocation = locations.get(locations.size() - 1);
      Location location = lastThreadFlowLocation.getLocation();
      lineNumbers.add(location.getPhysicalLocation().getRegion().getStartLine());
    }
    return lineNumbers;
  }

  private static final Logger LOG = LoggerFactory.getLogger(JavaXssVisitorFactory.class);
}
