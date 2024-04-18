package io.codemodder;

import com.contrastsecurity.sarif.Fingerprints;
import com.contrastsecurity.sarif.Result;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/** Utility class for building keys for SARIF findings. */
public final class SarifFindingKeyUtil {

  private SarifFindingKeyUtil() {}

  /**
   * Builds a finding ID for a SARIF finding based on the provided result, file path, and line
   * number.
   */
  public static String buildFindingId(final Result result, final Path path, final int line) {
    // prefer the guid, then the correlation guid
    if (!StringUtils.isBlank(result.getGuid())) {
      return result.getGuid().trim();
    } else if (!StringUtils.isBlank(result.getCorrelationGuid())) {
      return result.getCorrelationGuid().trim();
    }

    // use a fingerprint, as at least that is some guarantee of uniqueness
    final Fingerprints fingerprints = result.getFingerprints();
    final Map<String, String> fingerPrintProperties =
        fingerprints != null ? fingerprints.getAdditionalProperties() : new HashMap<>();
    if (!fingerPrintProperties.isEmpty()) {
      Collection<String> values = fingerPrintProperties.values();
      return values.iterator().next();
    }

    // ultimate fallback, some composite ID that will not represent anything
    return String.format("%s-%s-%d", result.getRuleId(), path.getFileName(), line);
  }
}
