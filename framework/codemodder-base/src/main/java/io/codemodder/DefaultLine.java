package io.codemodder;

import java.util.Objects;

/**
 * Value type implementation of {@link Line}. We should replace this with a {@code record} when we
 * upgrade to JDK 16+. This is a temporary workaround to avoid depending on JDK 16+.
 */
final class DefaultLine implements Line {

  private final int number;
  private final String content;

  DefaultLine(int number, String content) {
    if (number <= 0) {
      throw new IllegalArgumentException("Line number must be positive");
    }
    this.number = number;
    this.content = Objects.requireNonNull(content);
  }

  @Override
  public int number() {
    return number;
  }

  @Override
  public String content() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultLine that = (DefaultLine) o;

    if (number != that.number) return false;
    return content.equals(that.content);
  }

  @Override
  public int hashCode() {
    int result = number;
    result = 31 * result + content.hashCode();
    return result;
  }
}
