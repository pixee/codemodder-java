package io.openpixee.java;

import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.CodemodChangeRecorder;

/**
 * Implementation of {@link ModifierVisitor} that does nothing in case we need to return a no-op
 * visitor.
 */
public final class DoNothingVisitor extends ModifierVisitor<CodemodChangeRecorder> {}
