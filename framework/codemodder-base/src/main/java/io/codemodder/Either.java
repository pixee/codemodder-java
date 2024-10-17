package io.codemodder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** An implementation of the Either monad. It holds a single value of two possible types. */
public class Either<L, R> {

  private final L leftValue;
  private final R rightValue;

  private Either(L left, R right) {
    this.leftValue = left;
    this.rightValue = right;
  }

  /** Returns {@code true} if the left value is present. */
  public boolean isLeft() {
    return leftValue != null;
  }

  /** Returns {@code true} if the right value is present. */
  public boolean isRight() {
    return leftValue == null;
  }

  /** Returns the left value, may be {@code null} */
  public L getLeft() {
    return leftValue;
  }

  /** Returns the right value, may be {@code null} */
  public R getRight() {
    return rightValue;
  }

  /** Returns an {@link Either} containing {@code value} as a left value. */
  public static <A, B> Either<A, B> left(A value) {
    return new Either<>(Objects.requireNonNull(value), null);
  }

  /** Returns an {@link Either} containing {@code value} as a right value. */
  public static <A, B> Either<A, B> right(B value) {
    return new Either<>(null, Objects.requireNonNull(value));
  }

  /** Applies {@code func} to the left value if present. */
  public <A> Either<A, R> map(Function<L, A> func) {
    if (this.isLeft()) return Either.left(func.apply(this.leftValue));
    else return Either.right(this.rightValue);
  }

  /**
   * Applies {@code func} to the right value if present and returns an {@code Either} containing the
   * result. Otherwise, returns an {@code Either} containing the left value.
   */
  public <A> Either<A, R> flatMap(Function<L, Either<A, R>> func) {
    if (this.isLeft()) {
      var other = func.apply(this.leftValue);
      if (other.isLeft()) return Either.left(other.leftValue);
      else return Either.right(other.rightValue);
    } else return Either.right(this.rightValue);
  }

  /** Applies {@code func} to the left value if present. */
  public <A> Either<L, A> mapRight(Function<R, A> func) {
    if (this.isRight()) return Either.right(func.apply(this.rightValue));
    else return Either.left(this.leftValue);
  }

  /**
   * Applies {@code func} to the right value if present and returns an {@code Either} containing the
   * result. Otherwise, returns an {@code Either} containing the left value.
   */
  public <A> Either<L, A> flatMapRight(Function<R, Either<L, A>> func) {
    if (this.isRight()) {
      var other = func.apply(this.rightValue);
      if (other.isRight()) return Either.right(other.rightValue);
      else return Either.left(other.leftValue);
    } else return Either.left(this.leftValue);
  }

  /**
   * Builds {@code Either<L,R>} from an {@link Optional<L>} where either it contains the value of
   * the {@link Optional<L>} or the {@code orElse} object.
   */
  public static <L, R> Either<L, R> fromOptional(Optional<L> maybe, R orElse) {
    return maybe.<Either<L, R>>map(Either::left).orElseGet(() -> Either.right(orElse));
  }

  /**
   * If it contains the left value, applies the {@link Predicate} {@code pred} and return an {@code
   * Either} containing {@code orElse} if it fails. Otherwise do nothing
   */
  public Either<L, R> filter(Predicate<L> pred, R orElse) {
    if (this.isLeft()) {
      if (pred.test(this.leftValue)) return this;
      else return Either.right(orElse);
    } else return this;
  }

  /**
   * Applies the {@link Consumer} {@code leftAction} if it contains the left value, or the {@code
   * rightAction} otherwise.
   */
  public void ifLeftOrElse(Consumer<? super L> leftAction, Consumer<? super R> rightAction) {
    if (isLeft()) {
      leftAction.accept(getLeft());
    } else {
      rightAction.accept(getRight());
    }
  }

  /** Returns the result of the {@link Function}s {@code leftFunction} or {@code rightFunction}. */
  public <A> A ifLeftOrElseGet(
      Function<? super L, A> leftFunction, Function<? super R, A> rightFunction) {
    if (isLeft()) {
      return leftFunction.apply(getLeft());
    } else {
      return rightFunction.apply(getRight());
    }
  }

  @Override
  public String toString() {
    return "Either[" + (this.isLeft() ? this.leftValue : this.rightValue) + "]";
  }
}
