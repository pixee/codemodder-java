package io.openpixee.java;

import io.codemodder.ChangedFile;
import io.codemodder.CodemodChangeRecorder;
import io.codemodder.WeavingResult;
import java.io.File;
import java.util.Set;

/**
 * Given a repository root, give an extension the chance to make any arbitrary change to any
 * arbitrary file. This type should be extended if you don't want to change Java code like you can
 * in {@link VisitorFactory}.
 */
public interface FileBasedVisitor {

  /**
   * Given a repository root and some context, weave the changes you'd like to the configuration or
   * non-Java code.
   */
  WeavingResult visitRepositoryFile(
      File repositoryRoot,
      File file,
      CodemodChangeRecorder weavingContext,
      Set<ChangedFile> changedJavaFiles);

  String ruleId();
}
