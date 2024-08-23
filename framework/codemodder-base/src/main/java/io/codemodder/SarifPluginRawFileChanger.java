package io.codemodder;

import com.contrastsecurity.sarif.Result;
import java.nio.file.Path;
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
  public boolean supports(final Path file) {
    return !sarif.getResultsByLocationPath(file).isEmpty();
  }

  @Override
  public CodemodFileScanningResult visitFile(final CodemodInvocationContext context) {
    return onFileFound(context, sarif.getResultsByLocationPath(context.path()));
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
