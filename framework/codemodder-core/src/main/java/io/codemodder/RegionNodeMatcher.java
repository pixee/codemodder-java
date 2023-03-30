package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

/**
 * A type that can match a {@link Region} to a {@link Range} for determining if we should change the
 * node at this location.
 */
public interface RegionNodeMatcher {

  /** Return true if the given {@link Region} matches the given {@link Range}. */
  boolean matches(Region region, Range range);

    /**
     * Return true if the {@link Node} and {@link Region} start and end at the same location. Some
     * SARIF providers seem report an end column that is +1 more than you think -- the spec probably
     * says the value is exclusive or something.
     */
  RegionNodeMatcher EXACT_MATCH =
      (region, range) -> {
        return region.getStartLine() == range.begin.line
            && region.getStartColumn() == range.begin.column
            && region.getEndLine() == range.end.line
            && (region.getEndColumn() == range.end.column + 1
                || region.getEndColumn() == range.end.column);
      };

    /** Return true if the {@link Node} is {@link Region} start at the same location. */
  RegionNodeMatcher MATCHES_START =
      (region, range) -> {
        return region.getStartLine() == range.begin.line
            && region.getStartColumn() == range.begin.column;
      };
}
