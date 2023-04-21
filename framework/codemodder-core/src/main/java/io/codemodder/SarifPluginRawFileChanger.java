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

  /**
   * Creates a visitor for the given context and results.
   *
   * @param context the context of this files transformation
   * @param results the given SARIF results to act on
   * @return a {@link List} of {@Link Weave}s representing changes in the file.
   */
  public abstract List<Weave> onFileFound(CodemodInvocationContext context, List<Result> results);
}
