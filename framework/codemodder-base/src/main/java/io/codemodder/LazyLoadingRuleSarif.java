package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.inject.Provider;

/**
 * A {@link RuleSarif} that lazily loads an underlying {@link RuleSarif} when needed. This can be
 * used to delay execution.
 */
public class LazyLoadingRuleSarif implements RuleSarif {

  private RuleSarif ruleSarif;
  private final Provider<RuleSarif> ruleSarifProvider;

  public LazyLoadingRuleSarif(final Provider<RuleSarif> ruleSarifProvider) {
    this.ruleSarif = null; // not initialized
    this.ruleSarifProvider = Objects.requireNonNull(ruleSarifProvider);
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    checkInitialized();
    return ruleSarif.getRegionsFromResultsByRule(path);
  }

  private void checkInitialized() {
    if (ruleSarif == null) {
      ruleSarif = ruleSarifProvider.get();
      if (ruleSarif == null) {
        throw new IllegalStateException("SARIF must be provided");
      }
    }
  }

  @Override
  public List<Result> getResultsByLocationPath(final Path path) {
    checkInitialized();
    return ruleSarif.getResultsByLocationPath(path);
  }

  @Override
  public SarifSchema210 rawDocument() {
    checkInitialized();
    return ruleSarif.rawDocument();
  }

  @Override
  public String getRule() {
    checkInitialized();
    return ruleSarif.getRule();
  }

  @Override
  public String getDriver() {
    checkInitialized();
    return ruleSarif.getDriver();
  }
}
