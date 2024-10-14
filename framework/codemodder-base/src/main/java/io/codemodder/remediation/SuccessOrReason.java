package io.codemodder.remediation;

import io.codemodder.DependencyGAV;
import io.codemodder.Either;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a strategy fix. Either a list of dependencies to be added, or a reason
 * for failure.
 */
public class SuccessOrReason {

  private final Either<List<DependencyGAV>, String> either;

  SuccessOrReason(final List<DependencyGAV> deps) {
    this.either = Either.left(Objects.requireNonNull(deps));
  }

  SuccessOrReason(final String reason) {
    this.either = Either.right(Objects.requireNonNull(reason));
  }

  public boolean isSuccess() {
    return either.isLeft();
  }

  public List<DependencyGAV> getDependencies() {
    if (!isSuccess()) {
      throw new RuntimeException("Trying to get dependencies from a failure result");
    }
    return either.getLeft();
  }

  public String getReason() {
    if (isSuccess()) {
      throw new RuntimeException("Trying to get a reason from a successful result");
    }
    return either.getRight();
  }

  public static SuccessOrReason fromSuccess(final List<DependencyGAV> deps) {
    return new SuccessOrReason(deps);
  }

  public static SuccessOrReason fromSuccess() {
    return new SuccessOrReason(List.of());
  }

  public static SuccessOrReason fromFailure(final String reason) {
    return new SuccessOrReason(reason);
  }
}
