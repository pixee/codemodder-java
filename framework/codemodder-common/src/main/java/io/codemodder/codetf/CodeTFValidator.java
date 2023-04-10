package io.codemodder.codetf;

import static java.util.Collections.emptyMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Holds small utility for validating strings and immutable collections. */
final class CodeTFValidator {

  /**
   * Returns the given {@link String} if it passes the two requirements -- it's non-null and it's
   * not only whitespace.
   */
  static String requireNonBlank(final String s) {
    if (s == null) {
      throw new IllegalArgumentException("string can't be null");
    }
    if (s.isBlank()) {
      throw new IllegalArgumentException("string can't be blank");
    }
    return s;
  }

  /**
   * Given a {@link Map} that is possibly null, return an instance that represents an immutable copy
   * of it, or an empty map.
   */
  static <K, V> Map<K, V> toImmutableCopyOrEmptyOnNull(final Map<K, V> givenMap) {
    if (givenMap == null) {
      return emptyMap();
    }
    return Collections.unmodifiableMap(givenMap);
  }

  /**
   * Given a {@link List} that is possibly null, return an instance that represents an immutable
   * copy of it, or an empty list.
   */
  static <T> List<T> toImmutableCopyOrEmptyOnNull(final List<T> givenList) {
    if (givenList == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(givenList);
  }
}
