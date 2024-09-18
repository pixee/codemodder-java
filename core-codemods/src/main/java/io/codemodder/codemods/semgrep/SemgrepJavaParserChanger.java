package io.codemodder.codemods.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.RuleSarif;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.Objects;

/** A JavaParser changer for Contrast codemods. */
public abstract class SemgrepJavaParserChanger extends JavaParserChanger
    implements FixOnlyCodeChanger {

  protected final RuleSarif ruleSarif;

  protected SemgrepJavaParserChanger(
      final CodemodReporterStrategy reporter, final RuleSarif ruleSarif) {
    super(reporter);
    this.ruleSarif = Objects.requireNonNull(ruleSarif);
  }

  @Override
  public String vendorName() {
    return "Semgrep";
  }

  @Override
  public boolean shouldRun() {
    return hasResultsForRule(ruleSarif);
  }

  /** Returns true if the given SARIF document contains results for the given rule. */
  private static boolean hasResultsForRule(final RuleSarif ruleSarif) {
    // consider AppScanJavaParserChanger.shouldRun when modifying this
    SarifSchema210 rawSarif = ruleSarif.rawDocument();
    if (rawSarif != null && rawSarif.getRuns() != null && !rawSarif.getRuns().isEmpty()) {
      return rawSarif.getRuns().get(0).getResults().stream()
          .anyMatch(r -> r.getRuleId().equals(ruleSarif.getRule()));
    }
    return false;
  }
}
