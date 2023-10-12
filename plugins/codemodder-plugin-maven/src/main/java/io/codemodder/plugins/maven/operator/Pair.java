package io.codemodder.plugins.maven.operator;

class Pair<K, V> {
  private final K first;
  private final V second;

  Pair(K first, V second) {
    this.first = first;
    this.second = second;
  }

  K getFirst() {
    return first;
  }

  V getSecond() {
    return second;
  }
}
