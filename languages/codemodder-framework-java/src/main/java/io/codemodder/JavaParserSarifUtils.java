package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

/** Holds common AST utilities in JavaParser. */
public final class JavaParserSarifUtils {

  private JavaParserSarifUtils() {}

  /**
   * Return true if the {@link Node} is within the {@link Region} described.
   *
   * @param node the node to search for within the boundary
   * @param region the given region that defines the boundary
   * @return true, if the node is within the region
   */
  public static boolean regionMatchesNode(final Node node, final Region region) {
    Range sarifRange =
        Range.range(
            region.getStartLine(),
            region.getStartColumn(),
            region.getEndLine(),
            region.getEndColumn());
    Range observedRange = node.getRange().get();
    return observedRange.overlapsWith(sarifRange);
  }

  /**
   * Return true if the {@link Node} is {@link Region} start at the same location.
   *
   * @param node the AST node to compare
   * @param region the SARIF region to compare
   * @return true, if the two locations have equivalent start line and columns
   */
  public static boolean regionMatchesNodeStart(final Node node, final Region region) {
    Position position = node.getRange().get().begin;
    return region.getStartLine() == position.line && region.getStartColumn() == position.column;
  }
}
