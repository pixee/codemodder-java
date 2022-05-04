package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import java.io.File;

/**
 * This type represents something that wants to "change" (or just visit...) code and how people
 * extend our product to add more fixes.
 */
public interface VisitorFactory {

  /** Given some context, create a visitor for the given {@link CompilationUnit}. */
  ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(File file, CompilationUnit cu);

  String ruleId();
}
