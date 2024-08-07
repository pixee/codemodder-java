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
  boolean matches(SourceCodeRegion region, Range range);

  /**
   * Return true if the {@link Node} and {@link Region} start and end at the same location. Some
   * SARIF providers seem report an end column that is +1 more than you think -- the spec probably
   * says the value is exclusive or something.
   */
  RegionNodeMatcher EXACT_MATCH =
      (region, range) ->
          region.start().line() == range.begin.line
              && region.start().column() == range.begin.column
              && (region.end().line() != null ? region.end().line() : region.start().line())
                  == range.end.line
              && (region.end().column() == range.end.column + 1
                  || region.end().column() == range.end.column);

  /** Return true if the {@link Node} is {@link Region} start at the same location. */
  RegionNodeMatcher MATCHES_START =
      (region, range) ->
          region.start().line() == range.begin.line
              && region.start().column() == range.begin.column;

  RegionNodeMatcher MATCHES_LINE = (region, range) -> region.start().line() == range.begin.line;

  /**
   * Return true when the given {@link Region} is inside the given {@link Range}.
   *
   * <p>This may be used when the SARIF provider reports a region that is smaller than expected
   * (e.g. only the method name instead of the whole method expression). In these cases, the codemod
   * developer should take extra care to make sure the method expression matches expectations,
   * because this matcher may match multiple nodes erroneously.
   */
  RegionNodeMatcher REGION_INSIDE_RANGE =
      (region, range) ->
          region.start().line() >= range.begin.line
              && region.start().column() >= range.begin.column
              && region.end().line() <= range.end.line
              && region.end().column() <= range.end.column;
}
