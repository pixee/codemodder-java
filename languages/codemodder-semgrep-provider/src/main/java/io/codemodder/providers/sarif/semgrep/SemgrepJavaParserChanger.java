package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.*;
import java.util.List;
import java.util.Objects;

/** Provides base functionality for making JavaParser-based changes with Semgrep. */
public abstract class SemgrepJavaParserChanger<T extends Node> implements JavaParserChanger {

  protected final RuleSarif sarif;
  private final Class<? extends Node> nodeType;
  private final RegionExtractor regionExtractor;

  protected SemgrepJavaParserChanger(
      final RuleSarif semgrepSarif, final Class<? extends Node> nodeType) {
    this(semgrepSarif, nodeType, RegionExtractor.FROM_FIRST_LOCATION);
  }

  protected SemgrepJavaParserChanger(
      final RuleSarif semgrepSarif,
      final Class<? extends Node> nodeType,
      final RegionExtractor regionExtractor) {
    this.sarif = Objects.requireNonNull(semgrepSarif);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionExtractor = Objects.requireNonNull(regionExtractor);
  }

  @Override
  public final void visit(final CodemodInvocationContext context, final CompilationUnit cu) {

    List<Result> results = sarif.getResultsByPath(context.path());
    List<? extends Node> allNodes = cu.findAll(nodeType);

    for (Result result : results) {
      for (Node node : allNodes) {
        Region region = regionExtractor.from(result);
        if (!node.getClass().isAssignableFrom(nodeType)) {
          continue;
        }
        FileWeavingContext changeRecorder = context.changeRecorder();
        if (changeRecorder.isLineIncluded(region.getStartLine())
            && JavaParserSarifUtils.regionMatchesNodeStart(node, region)) {
          boolean changeSuccessful = onSemgrepResultFound(context, cu, (T) node, result);
          if (changeSuccessful) {
            changeRecorder.addWeave(
                Weave.from(region.getStartLine(), context.codemodId(), dependenciesRequired()));
          }
        }
      }
    }
  }

  /**
   * Creates a visitor for the given context and locations.
   *
   * @param context the context of this files transformation
   * @param cu the parsed model of the file being transformed
   * @param node the node to act on
   * @param result the given SARIF result to act on
   * @return true, if the change was made, false otherwise
   */
  public abstract boolean onSemgrepResultFound(
      CodemodInvocationContext context, CompilationUnit cu, T node, Result result);
}
