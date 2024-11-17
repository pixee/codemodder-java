package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodeDirectory;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.util.*;

/** A factory for building {@link AppScanRuleSarif}s. */
public final class AppScanRuleSarifFactory implements RuleSarifFactory {

  /** A map of a AppScan SARIF "location" URIs mapped to their respective file paths. */
  private final Map<SarifSchema210, AppScanSarifLocationData> sarifLocationDataCache;

  public AppScanRuleSarifFactory() {
    this.sarifLocationDataCache = new HashMap<>();
  }

  @Override
  public Optional<RuleSarif> build(
      final String toolName,
      final String rule,
      final String messageText,
      final SarifSchema210 sarif,
      final CodeDirectory codeDirectory) {
    if (AppScanRuleSarif.toolName.equals(toolName)) {
      AppScanSarifLocationData sarifLocationData = sarifLocationDataCache.get(sarif);
      if (sarifLocationData == null) {
        sarifLocationData = new AppScanSarifLocationData(sarif, codeDirectory);
        sarifLocationDataCache.put(sarif, sarifLocationData);
      }
      return Optional.of(new AppScanRuleSarif(messageText, sarif, sarifLocationData));
    }
    return Optional.empty();
  }
}
