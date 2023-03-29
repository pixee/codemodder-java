package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.java.DoNothingVisitor;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fixes issues reported under the id "java/stack-trace-exposure" */
final class StackTraceExposureVisitorFactory implements VisitorFactory {

  private final Set<Result> results;
  private final String repositoryRootPath;

  StackTraceExposureVisitorFactory(final File repositoryRoot, final Set<Result> results) {
    this.results = Objects.requireNonNull(results, "results");
    try {
      this.repositoryRootPath =
          Objects.requireNonNull(repositoryRoot.getCanonicalPath(), "repositoryRootPath");
    } catch (IOException e) {
      throw new IllegalArgumentException("Bad path for " + repositoryRoot, e);
    }
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    for (Result result : results) {
      List<Location> locations = result.getLocations();
      for (final Location location : locations) {
        try {
          if (looksTheSame(location.getPhysicalLocation(), file)) {
            return new StackTraceExposureModifierVisitor(location);
          }
        } catch (IOException e) {
          LOG.error("Problem assessing if rule matches file: {}", file, e);
        }
      }
    }
    return new DoNothingVisitor();
  }

  @Override
  public String ruleId() {
    return stackTraceExposureRuleId;
  }

  private boolean looksTheSame(final PhysicalLocation physicalLocation, final File file)
      throws IOException {
    String filePath = file.getCanonicalPath();
    String fileUri = physicalLocation.getArtifactLocation().getUri();
    return filePath.startsWith(repositoryRootPath) && filePath.endsWith(fileUri);
  }

  /** Replaces calls to HttpServletResponse#sendError(int, String) to sendError(int). */
  private static class StackTraceExposureModifierVisitor
      extends ModifierVisitor<FileWeavingContext> {
    private final Location location;

    private StackTraceExposureModifierVisitor(final Location location) {
      this.location = Objects.requireNonNull(location);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if ("sendError".equals(methodCallExpr.getNameAsString())
          && methodCallExpr.getArguments().size() == 2) {
        Region findingRegion = location.getPhysicalLocation().getRegion();
        Optional<Range> expRange = methodCallExpr.getRange();
        if (expRange.isPresent()
            && context.isLineIncluded(methodCallExpr.getRange().get().begin.line)) {
          int startLine = expRange.get().begin.line;
          if (startLine == findingRegion.getStartLine()) {
            NodeList<Expression> newArguments = NodeList.nodeList(methodCallExpr.getArgument(0));
            methodCallExpr.setArguments(newArguments);
            context.addWeave(Weave.from(startLine, stackTraceExposureRuleId));
          }
        }
      }
      return methodCallExpr;
    }
  }

  private static final String stackTraceExposureRuleId = "codeql:java/stack-trace-exposure";
  private static final Logger LOG = LoggerFactory.getLogger(StackTraceExposureVisitorFactory.class);
}
