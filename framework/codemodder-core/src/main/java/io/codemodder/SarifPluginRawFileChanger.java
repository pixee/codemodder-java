package io.codemodder;

import com.contrastsecurity.sarif.Result;
import java.io.IOException;
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
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
    List<Result> results = sarif.getResultsByPath(context.path());
    if (!results.isEmpty()) {
      List<CodemodChange> allChanges = onFileFound(context, results);
      return allChanges;
    }
    return List.of();
  }

  /**
   * Creates a visitor for the given context and results.
   *
   * @param context the context of this files transformation
   * @param results the given SARIF results to act on
   * @return a {@link List} of {@Link CodemodChange}s representing changes in the file.
   */
  public abstract List<CodemodChange> onFileFound(
      CodemodInvocationContext context, List<Result> results);
}
