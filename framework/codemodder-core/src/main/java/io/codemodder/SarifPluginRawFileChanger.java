package io.codemodder;

import com.contrastsecurity.sarif.Result;
import java.io.IOException;
import java.util.List;

/** A {@link RawFileChanger} bundled with a {@link RuleSarif}. */
public abstract class SarifPluginRawFileChanger implements RawFileChanger {

  private final RuleSarif sarif;

  protected SarifPluginRawFileChanger(final RuleSarif sarif) {
    this.sarif = sarif;
  }

  @Override
  public void visitFile(final CodemodInvocationContext context) throws IOException {
    List<Result> results = sarif.getResultsByPath(context.path());
    if (!results.isEmpty()) {
      List<Weave> allChanges = onFileFound(context, results);
      allChanges.stream().forEach(weave -> context.changeRecorder().addWeave(weave));
    }
  }

  public abstract List<Weave> onFileFound(CodemodInvocationContext context, List<Result> results);
}
