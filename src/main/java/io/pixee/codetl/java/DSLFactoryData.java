package io.pixee.codetl.java;

import com.github.javaparser.ast.CompilationUnit;
import io.pixee.codefixer.java.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DSLFactoryData {

  private TransformationType transformationType;
  private String ruleId;
  private final List<Function<CompilationUnit, Predicate<?>>> predicates = new ArrayList<>();

  private Transformer<?, ?> transformer;
  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }
  public void add(Function<CompilationUnit, Predicate<?>> predicate) {
    predicates.add(predicate);
  }

  public void add(List<Function<CompilationUnit, Predicate<?>>> predicate) {
    predicates.addAll(predicate);
  }

  public List<Function<CompilationUnit, Predicate<?>>> getPredicates() {
    return predicates;
  }

  public Transformer<?, ?> getTransformer() {
    return transformer;
  }

  public void setTransformer(Transformer<?, ?> transformer) {
    this.transformer = transformer;
  }


  public TransformationType getTransformationType() {
    return transformationType;
  }

  public void setTransformationType(TransformationType transformationType) {
    this.transformationType = transformationType;
  }
}
