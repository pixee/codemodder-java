package io.codemodder.codemods;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class Either<L, R> {

  private final L leftValue;
  private final R rightValue;

  private Either(L left, R right) {
    this.leftValue = left;
    this.rightValue = right;
  }

  public boolean isLeft() {
    return leftValue != null;
  }

  public boolean isRight() {
    return leftValue == null;
  }

  public L getLeft() {
    return leftValue;
  }

  public R getRight() {
    return rightValue;
  }

  public static <A, B> Either<A, B> left(A value) {
    return new Either<>(value, null);
  }

  public static <A, B> Either<A, B> right(B value) {
    return new Either<>(null, value);
  }

  public <A> Either<A, R> map(Function<L, A> func) {
    if (this.isLeft()) return new Either<>(func.apply(this.leftValue), null);
    else return new Either<>(null, this.rightValue);
  }

  public <A> Either<A, R> flatMap(Function<L, Either<A, R>> func) {
    if (this.isLeft()) {
      var other = func.apply(this.leftValue);
      if (other.isLeft()) return new Either<>(other.leftValue, null);
      else return new Either<>(null, other.rightValue);
    } else return new Either<>(null, this.rightValue);
  }

  public <A> Either<L, A> mapRight(Function<R, A> func) {
    if (this.isRight()) return new Either<>(null, func.apply(this.rightValue));
    else return new Either<>(this.leftValue, null);
  }

  public <A> Either<L, A> flatMapRight(Function<R, Either<L, A>> func) {
    if (this.isRight()) {
      var other = func.apply(this.rightValue);
      if (other.isRight()) return new Either<>(null, other.rightValue);
      else return new Either<>(other.leftValue, null);
    } else return new Either<>(this.leftValue, null);
  }

  public static <L, R> Either<L, R> fromOptional(Optional<L> maybe, R orElse) {
    if (maybe.isPresent()) return Either.left(maybe.get());
    else return Either.right(orElse);
  }

  public Either<L, R> filter(Predicate<L> pred, R orElse) {
    if (this.isLeft()) {
      if (pred.test(this.leftValue)) return this;
      else return Either.right(orElse);
    } else return this;
  }

  public void ifPresentOrElse(Consumer<? super L> leftAction, Consumer<? super R> rightAction) {
    if (isLeft()) {
      leftAction.accept(getLeft());
    } else {
      rightAction.accept(getRight());
    }
  }

  public String toString() {
    return "Either[" + (this.isLeft() ? this.leftValue : this.rightValue) + "]";
  }
}
