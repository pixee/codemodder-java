package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.google.gson.GsonBuilder;
import io.codescan.sarif.model.*;
import io.pixee.codefixer.java.DoNothingVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Fixes things discovered by PMD.
 *
 * @deprecated this needs to be broken down into multiple visitors so they can be controlled
 */
@Deprecated
public final class PMDVisitorFactory implements VisitorFactory {

  private final Map<String, List<Result>> resultsByFile;

  public PMDVisitorFactory(final File pmdResultsFile) {
    if (pmdResultsFile == null
        || !pmdResultsFile.exists()
        || !pmdResultsFile.isFile()
        || !pmdResultsFile.canRead()) {
      throw new IllegalArgumentException("pmd file must be non-null and readable file");
    }
    try {
      SarifLog sarifLog =
          new GsonBuilder().create().fromJson(new FileReader(pmdResultsFile), SarifLog.class);
      this.resultsByFile = new HashMap<>();
      Run run = sarifLog.getRuns().get(0);
      List<Result> results = run.getResults();
      for (Result result : results) {
        Location location = result.getLocations().get(0);
        PhysicalLocation physicalLocation = location.getPhysicalLocation();
        ArtifactLocation artifactLocation = physicalLocation.getArtifactLocation();
        String fileUri = artifactLocation.getUri();
        List<Result> resultsForFile =
            resultsByFile.computeIfAbsent(fileUri, k -> new ArrayList<>());
        resultsForFile.add(result);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("couldn't deserialize to SARIF model objects", e);
    }
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    final List<Result> pmdResults = resultsByFile.get(file.getAbsolutePath());
    if (pmdResults == null || pmdResults.isEmpty()) {
      return new DoNothingVisitor();
    }
    return new PMDVisitor(cu, pmdResults);
  }

  @Override
  public String ruleId() {
    throw new UnsupportedOperationException("unsupported per rule id");
  }

  private static final class PMDVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;
    private final List<Result> pmdResults;

    private PMDVisitor(final CompilationUnit cu, final List<Result> pmdResults) {
      this.cu = cu;
      this.pmdResults = Objects.requireNonNull(pmdResults);
    }

    @Override
    public Visitable visit(MethodCallExpr n, final FileWeavingContext context) {
      // LiteralsFirstInComparisons -- e.g., don't call nullable.equals("foo"), prefer
      // "foo".equals(nullable)
      return super.visit(n, context);
    }

    @Override
    public Visitable visit(ObjectCreationExpr n, final FileWeavingContext context) {
      // PrimitiveWrapperInstantiation -- e.g, don't call new Boolean(false), prefer `Boolean.FALSE`
      return super.visit(n, context);
    }
  }
}
