package io.pixee.codetl.java;

import io.pixee.codefixer.java.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class VisitorFactoryData<T> {
  private final List<Predicate<T>> predicates = new ArrayList<>();

  private Transformer<T, T> creationExpr;

  public void add(Predicate<T> predicate) {
    predicates.add(predicate);
  }

  public void add(List<Predicate<T>> predicate) {
    predicates.addAll(predicate);
  }

  public List<Predicate<T>> getPredicates() {
    return predicates;
  }

  public Transformer<T, T> getCreationExpr() {
    return creationExpr;
  }

  public void setCreationExpr(Transformer<T, T> creationExpr) {
    this.creationExpr = creationExpr;
  }
}
