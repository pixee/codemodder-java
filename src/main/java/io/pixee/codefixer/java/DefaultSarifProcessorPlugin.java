package io.pixee.codefixer.java;

import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.Tool;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * A default {@link SarifProcessorPlugin} to extend that breaks apart the "do I support this SARIF"
 * question independently for ease of use.
 */
public abstract class DefaultSarifProcessorPlugin implements SarifProcessorPlugin {

  public abstract boolean supports(Tool tool);

  @Override
  public final List<VisitorFactory> getJavaVisitorFactoriesFor(
      final File repositoryRoot, final Run run, final RuleContext ruleContext) {
    if (supports(run.getTool())) {
      return getVendorToolSpecificFactories(repositoryRoot, run, ruleContext);
    }
    return Collections.emptyList();
  }

  protected abstract List<VisitorFactory> getVendorToolSpecificFactories(
      File repositoryRoot, Run run, RuleContext ruleContext);
}
