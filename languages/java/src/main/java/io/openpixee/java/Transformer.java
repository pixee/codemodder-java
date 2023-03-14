package io.openpixee.java;

import io.codemodder.FileWeavingContext;
import io.openpixee.java.protections.TransformationResult;

public interface Transformer<T, Y> {

  TransformationResult<Y> transform(T node, final FileWeavingContext context)
      throws TransformationException;
}
