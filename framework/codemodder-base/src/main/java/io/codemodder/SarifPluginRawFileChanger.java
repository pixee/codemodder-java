package io.codemodder;

import com.contrastsecurity.sarif.Result;
import java.util.List;
import java.util.Set;

/** A {@link RawFileChanger} bundled with a {@link RuleSarif}. */
public abstract class SarifPluginRawFileChanger extends RawFileChanger {

  private final RuleSarif sarif;

  private final IncludesExcludesPattern includesExcludesPattern;

  protected SarifPluginRawFileChanger(final RuleSarif sarif) {
    this.sarif = sarif;
    this.includesExcludesPattern = new IncludesExcludesPattern.Default(sarif.getPaths(), Set.of());
  }

  protected SarifPluginRawFileChanger(
      final RuleSarif sarif, final CodemodReporterStrategy reporter) {
    super(reporter);
    this.sarif = sarif;
    this.includesExcludesPattern = new IncludesExcludesPattern.Default(sarif.getPaths(), Set.of());
  }

  @Override
  public CodemodFileScanningResult visitFile(final CodemodInvocationContext context) {
    List<Result> results = sarif.getResultsByLocationPath(context.path());
    if (!results.isEmpty()) {
      return onFileFound(context, results);
    }
    return CodemodFileScanningResult.none();
  }

  /**
   * Creates a visitor for the given context and results.
   *
   * @param context the context of this files transformation
   * @param results the given SARIF results to act on
   * @return a {@link List} of {@link CodemodChange}s representing changes in the file.
   */
  public abstract CodemodFileScanningResult onFileFound(
      CodemodInvocationContext context, List<Result> results);
}
