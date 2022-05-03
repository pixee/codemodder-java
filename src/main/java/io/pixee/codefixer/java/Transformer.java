package io.pixee.codefixer.java;

import io.pixee.codefixer.java.protections.TransformationResult;

public interface Transformer<T> {

    TransformationResult<T> transform(T node, final FileWeavingContext context) throws TransformationException;
}
