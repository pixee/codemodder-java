package io.codemodder;

import java.util.Objects;

/** Represents a position in a source file. */
public final class Position {
  private int line;
  private int column;

  public Position(int line, int column) {
    this.line = line;
    this.column = column;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Position position = (Position) o;
    return line == position.line && column == position.column;
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, column);
  }
}
