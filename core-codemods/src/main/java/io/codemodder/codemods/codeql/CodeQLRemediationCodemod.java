package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.RuleSarif;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.Objects;

/** Base class for CodeQL remediation codemods. */
public abstract class CodeQLRemediationCodemod extends JavaParserChanger
    implements FixOnlyCodeChanger {

  protected final RuleSarif ruleSarif;

  protected CodeQLRemediationCodemod(
      final CodemodReporterStrategy reporter, final RuleSarif ruleSarif) {
    super(reporter);
    this.ruleSarif = Objects.requireNonNull(ruleSarif);
  }

  @Override
  public String vendorName() {
    return "CodeQL";
  }

  @Override
  public boolean shouldRun() {
    return hasResultsForRule(ruleSarif);
  }

  /** Returns true if the given SARIF document contains results for the given rule. */
  private static boolean hasResultsForRule(final RuleSarif ruleSarif) {
    SarifSchema210 rawSarif = ruleSarif.rawDocument();
    if (rawSarif != null && rawSarif.getRuns() != null && !rawSarif.getRuns().isEmpty()) {
      return rawSarif.getRuns().get(0).getResults().stream()
          .anyMatch(r -> r.getRuleId().equals(ruleSarif.getRule()));
    }
    return false;
  }
}
