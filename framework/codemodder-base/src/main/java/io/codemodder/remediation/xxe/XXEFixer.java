package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;

/** Interface for fixing XXEs. */
interface XXEFixer {

  /** A provider (for a given XML API) attempts to fix the given issue. */
  XXEFixAttempt tryFix(int line, Integer column, CompilationUnit cu);
}
