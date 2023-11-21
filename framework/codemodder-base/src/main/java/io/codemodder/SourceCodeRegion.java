package io.codemodder;

import com.contrastsecurity.sarif.Region;
import java.util.Objects;

/** Describes a region of source code. */
public record SourceCodeRegion(Position start, Position end) {

  public SourceCodeRegion {
    Objects.requireNonNull(start);
    Objects.requireNonNull(end);
  }

  /** Translate the SARIF model into the common model. */
  public static SourceCodeRegion fromSarif(final Region region) {
    Position start = new Position(region.getStartLine(), region.getStartColumn());
    Position end = new Position(region.getEndLine(), region.getEndColumn());
    return new SourceCodeRegion(start, end);
  }
}
