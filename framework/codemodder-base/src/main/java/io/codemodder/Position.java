package io.codemodder;

import java.util.Objects;

/** Represents a position in a source file. */
public record Position(Integer line, Integer column) {

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Position position = (Position) o;
    return Objects.equals(line, position.line) && Objects.equals(column, position.column);
  }
}
