package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;

/** Interface for fixing XXEs. */
interface XXEFixer {

  /** A provider (for a given XML API) attempts to fix the given issue. */
  <T> XXEFixAttempt tryFix(T issue, int line, Integer column, CompilationUnit cu);
}
