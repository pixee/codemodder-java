package io.codemodder;

import com.contrastsecurity.sarif.Fingerprints;
import com.contrastsecurity.sarif.Result;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Utility class for building keys for SARIF findings. */
public class SarifFindingKeyUtil {

  private SarifFindingKeyUtil() {}

  /** Builds a key for a SARIF finding based on the provided result, file path, and line number. */
  public static String buildKey(final Result result, final Path path, final int line) {
    final Fingerprints fingerprints = result.getFingerprints();

    final Map<String, String> fingerPrintProperties =
        fingerprints != null ? fingerprints.getAdditionalProperties() : new HashMap<>();

    if (fingerPrintProperties.isEmpty()) {
      return String.format("%s-%s-%d", result.getRuleId(), path.getFileName(), line);
    }

    Collection<String> values = fingerPrintProperties.values();

    return values.iterator().next();
  }
}
