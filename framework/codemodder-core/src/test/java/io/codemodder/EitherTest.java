package io.codemodder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EitherTest {

  @Test
  void it_excludes_the_other_value() {
    Either<Integer, String> left = Either.left(0);
    assertThat(left.isLeft() && !left.isRight(), is(true));
    Either<Integer, String> right = Either.right("");
    assertThat(!right.isLeft() && right.isRight(), is(true));
  }

  @Test
  void it_maps_correctly() {
    Either<Integer, String> left = Either.left(0);
    assertThat(left.map(i -> i + 1).getLeft(), is(1));
    Either<Integer, String> right = Either.right("");
    assertThat(right.mapRight(s -> s + "1").getRight(), is("1"));
  }

  @Test
  void it_flatmaps_correctly() {
    Either<Integer, String> left = Either.left(0);
    assertThat(left.flatMap(i -> Either.left(i + 1)).getLeft(), is(1));
    Either<Integer, String> right = Either.right("");
    assertThat(right.flatMapRight(s -> Either.right(s + "1")).getRight(), is("1"));
  }

  @Test
  void it_filters_correctly() {
    Either<Integer, String> left = Either.left(0);
    assertThat(left.filter(i -> i == 0, "").getLeft(), is(0));
    assertThat(left.filter(i -> i != 0, "").getRight(), is(""));
  }

  @Test
  void it_consumes_correctly() {
    List<Integer> list = new ArrayList<>();
    Either<Integer, String> left = Either.left(0);
    Either<Integer, String> right = Either.right("");
    left.ifLeftOrElse(i -> list.add(0), s -> list.add(1));
    right.ifLeftOrElse(i -> list.add(0), s -> list.add(1));
    assertThat(list, contains(0, 1));
  }
}
