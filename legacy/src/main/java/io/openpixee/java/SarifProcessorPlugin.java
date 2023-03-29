package io.openpixee.java;

import com.contrastsecurity.sarif.Run;
import io.codemodder.RuleContext;
import java.io.File;
import java.util.List;

/** A plugin that supports acting on the results of a SARIF-producing vendor. */
public interface SarifProcessorPlugin {

  /** Create a set of Java factories associated with the given SARIF {@link Run}. */
  List<VisitorFactory> getJavaVisitorFactoriesFor(
      File repositoryRoot, Run run, RuleContext ruleContext);

  /**
   * Create a set of file based weavers intended to handle vulnerabilities found in the given SARIF
   * {@link Run}.
   */
  List<FileBasedVisitor> getFileWeaversFor(File repositoryRoot, Run run, RuleContext ruleContext);
}
