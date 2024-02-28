package io.codemodder;

import com.contrastsecurity.sarif.Result;
import java.util.List;

/** A {@link RawFileChanger} bundled with a {@link RuleSarif}. */
public abstract class SarifPluginRawFileChanger extends RawFileChanger {

  private final RuleSarif sarif;

  protected SarifPluginRawFileChanger(final RuleSarif sarif) {
    this.sarif = sarif;
  }

  protected SarifPluginRawFileChanger(
      final RuleSarif sarif, final CodemodReporterStrategy reporter) {
    super(reporter);
    this.sarif = sarif;
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) {
    List<Result> results = sarif.getResultsByLocationPath(context.path());
    if (!results.isEmpty()) {
      return onFileFound(context, results);
    }
    return List.of();
  }

  /**
   * Creates a visitor for the given context and results.
   *
   * @param context the context of this files transformation
   * @param results the given SARIF results to act on
   * @return a {@link List} of {@link CodemodChange}s representing changes in the file.
   */
  public abstract List<CodemodChange> onFileFound(
      CodemodInvocationContext context, List<Result> results);
}
