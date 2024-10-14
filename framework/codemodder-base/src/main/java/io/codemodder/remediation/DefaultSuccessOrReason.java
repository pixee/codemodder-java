package io.codemodder.remediation;

import io.codemodder.DependencyGAV;
import io.codemodder.Either;
import java.util.List;
import java.util.NoSuchElementException;

class DefaultSuccessOrReason implements SuccessOrReason {

  private final Either<List<DependencyGAV>, String> either;

  DefaultSuccessOrReason(final List<DependencyGAV> dependencies) {
    this.either = Either.left(dependencies);
  }

  DefaultSuccessOrReason(final String reason) {
    this.either = Either.right(reason);
  }

  @Override
  public boolean isSuccess() {
    return either.isLeft();
  }

  @Override
  public List<DependencyGAV> getDependencies() {
    if (!isSuccess()) {
      throw new NoSuchElementException("Trying to get dependencies from a failure result");
    }
    return either.getLeft();
  }

  @Override
  public String getReason() {
    if (isSuccess()) {
      throw new NoSuchElementException("Trying to get a reason from a successful result");
    }
    return either.getRight();
  }
}
