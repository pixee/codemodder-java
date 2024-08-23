package io.codemodder;

import com.contrastsecurity.sarif.Result;
import org.apache.commons.lang3.StringUtils;

/** Utility class for building keys for SARIF findings. */
public final class SarifFindingKeyUtil {

  private SarifFindingKeyUtil() {}

  /**
   * Builds a finding ID for a SARIF finding based on the provided result.
   *
   * <p>Individual results are identified by the {@code guid} property, if present. Multiple results
   * across scans are identified by the {@code correlationGuid} property. We prefer to identify the
   * result by its {@code guid} if present, and fall back to the {@code correlationGuid} if not. We
   * can be reasonably certain that the {@code correlationGuid} is unique within a single {@code
   * run}.
   */
  public static String buildFindingId(final Result result) {
    if (!StringUtils.isBlank(result.getGuid())) {
      return result.getGuid().trim();
    } else if (!StringUtils.isBlank(result.getCorrelationGuid())) {
      return result.getCorrelationGuid().trim();
    }
    return null;
  }
}
