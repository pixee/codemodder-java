package io.pixee.codefixer.java.plugins.codeql;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.DoNothingVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Fixes issues reported under the id "java/missing-jwt-signature-check" */
final class UnverifiedJwtParseVisitorFactory implements VisitorFactory {

  /** The locations of each result. */
  private final List<PhysicalLocation> locations;

  /**
   * The root of the repository.
   */
  private final String repositoryRootPath;

  UnverifiedJwtParseVisitorFactory(final File repositoryRoot, final Set<Result> results) {
    Objects.requireNonNull(results, "results");
    this.locations =
        results.stream()
            .map(result -> result.getLocations().get(0).getPhysicalLocation())
            .collect(Collectors.toUnmodifiableList());
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
    for (PhysicalLocation location : locations) {
      try {
        if (looksTheSame(location, file)) {
          return new UnverifiedJwtParseModifierVisitor(getAllLocationsWithSameFile(location));
        }
      } catch (IOException e) {
        LOG.error("Problem assessing if rule matches file: {}", file, e);
      }
    }
    return new DoNothingVisitor();
  }

    @Override
    public String ruleId() {
        return missingJwtSignatureCheckRuleId;
    }

    private List<PhysicalLocation> getAllLocationsWithSameFile(final PhysicalLocation location) {
    return locations.stream()
        .filter(
            loc ->
                loc.getArtifactLocation().getUri().equals(location.getArtifactLocation().getUri()))
        .collect(Collectors.toUnmodifiableList());
  }

  private boolean looksTheSame(final PhysicalLocation physicalLocation, final File file)
      throws IOException {
    String filePath = file.getCanonicalPath();
    String fileUri = physicalLocation.getArtifactLocation().getUri();
    return filePath.startsWith(repositoryRootPath) && filePath.endsWith(fileUri);
  }

  /** Replaces calls to Jwt.parser().parse() with .parseClaimsJws() */
  private static class UnverifiedJwtParseModifierVisitor
      extends ModifierVisitor<FileWeavingContext> {
    private final List<PhysicalLocation> locations;

    private UnverifiedJwtParseModifierVisitor(final List<PhysicalLocation> locations) {
      this.locations = Objects.requireNonNull(locations);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if ("parse".equals(methodCallExpr.getNameAsString())
          && methodCallExpr.getArguments().size() == 1) {
        Optional<Range> expRange = methodCallExpr.getRange();
        if (expRange.isPresent() && context.isLineIncluded(methodCallExpr)) {
          boolean instrumentedThisMethodCall = false;
          for (int i = 0; i < locations.size() && !instrumentedThisMethodCall; i++) {
            PhysicalLocation location = locations.get(i);
            Region findingRegion = location.getRegion();
            int startLine = expRange.get().begin.line;
            Integer resultStartLine = findingRegion.getStartLine();
            if (startLine == resultStartLine) {
              methodCallExpr.setName("parseClaimsJws");
              context.addWeave(Weave.from(startLine, missingJwtSignatureCheckRuleId));
              instrumentedThisMethodCall = true;
            }
          }
        }
      }
      return methodCallExpr;
    }


  }

  private static final String missingJwtSignatureCheckRuleId = "codeql:java/missing-jwt-signature-check";
  private static final Logger LOG = LogManager.getLogger(UnverifiedJwtParseVisitorFactory.class);
}
