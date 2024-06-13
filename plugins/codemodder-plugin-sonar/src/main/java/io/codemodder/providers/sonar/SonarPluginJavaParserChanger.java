package io.codemodder.providers.sonar;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.*;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.sonar.model.SonarFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Provides base functionality for making JavaParser-based changes based on Sonar results. */
public abstract class SonarPluginJavaParserChanger<T extends Node, S extends SonarFinding>
    extends JavaParserChanger implements FixOnlyCodeChanger {

  private final RuleFinding<S> ruleFinding;
  private final Class<? extends Node> nodeType;
  private final RegionNodeMatcher regionNodeMatcher;

  private final NodeCollector nodeCollector;

  protected SonarPluginJavaParserChanger(
      final RuleFinding<S> ruleFinding,
      final Class<? extends Node> nodeType,
      final RegionNodeMatcher regionNodeMatcher,
      final NodeCollector nodeCollector) {
    this.ruleFinding = Objects.requireNonNull(ruleFinding);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionNodeMatcher = regionNodeMatcher;
    this.nodeCollector = nodeCollector;
  }

  protected SonarPluginJavaParserChanger(
      final RuleFinding<S> ruleFinding, final Class<? extends Node> nodeType) {
    this(ruleFinding, nodeType, RegionNodeMatcher.MATCHES_START, NodeCollector.ALL_FROM_TYPE);
  }

  protected SonarPluginJavaParserChanger(
      final RuleFinding<S> ruleFinding,
      final Class<? extends Node> nodeType,
      final RegionNodeMatcher regionNodeMatcher,
      final CodemodReporterStrategy codemodReporterStrategy) {
    super(codemodReporterStrategy);
    this.ruleFinding = Objects.requireNonNull(ruleFinding);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionNodeMatcher = regionNodeMatcher;
    this.nodeCollector = NodeCollector.ALL_FROM_TYPE;
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<? extends SonarFinding> findings = ruleFinding.getResultsByPath(context.path());

    // small shortcut to avoid always executing the expensive findAll
    if (findings == null || findings.isEmpty()) {
      return CodemodFileScanningResult.none();
    }
    final List<? extends Node> allNodes = nodeCollector.collectNodes(cu, nodeType);

    List<CodemodChange> codemodChanges = new ArrayList<>();
    for (SonarFinding sonarFinding : findings) {
      for (Node node : allNodes) {
        Position start =
            new Position(
                sonarFinding.getTextRange().getStartLine(),
                sonarFinding.getTextRange().getStartOffset() + 1);
        Position end =
            new Position(
                sonarFinding.getTextRange().getEndLine(),
                sonarFinding.getTextRange().getEndOffset() + 1);
        SourceCodeRegion region = new SourceCodeRegion(start, end);
        if (!nodeType.isAssignableFrom(node.getClass())) {
          continue;
        }
        if (context.lineIncludesExcludes().matches(region.start().line())) {
          if (node.getRange().isPresent()) {
            Range range = node.getRange().get();
            if (regionNodeMatcher.matches(region, range)) {
              ChangesResult changeSuccessful =
                  onFindingFound(context, cu, (T) node, (S) sonarFinding);

              if (changeSuccessful.areChangesApplied()) {
                codemodChanges.add(
                    CodemodChange.from(
                        region.start().line(),
                        changeSuccessful.getDependenciesRequired(),
                        new FixedFinding(sonarFinding.getKey(), this.detectorRule())));
              }
            }
          }
        }
      }
    }
    return CodemodFileScanningResult.withOnlyChanges(codemodChanges);
  }

  @Override
  public boolean shouldRun() {
    return ruleFinding.hasResults();
  }

  /**
   * Creates a visitor for the given context and locations.
   *
   * @param context the context of this files transformation
   * @param cu the parsed model of the file being transformed
   * @param node the node to act on
   * @param sonarFinding the given Sonar finding to act on
   * @return {@link ChangesResult}, that contains result changes
   */
  protected abstract ChangesResult onFindingFound(
      CodemodInvocationContext context, CompilationUnit cu, T node, S sonarFinding);

  @Override
  public String vendorName() {
    return "Sonar";
  }
}
