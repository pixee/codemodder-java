package io.codemodder;

import io.codemodder.codetf.DetectorFinding;
import java.util.List;

/** Represents the result of scanning a file for changes. */
public interface CodemodFileScanningResult {

  /** Creates a new instance of {@link CodemodFileScanningResult} from the given values. */
  static CodemodFileScanningResult from(
      final List<CodemodChange> changes, final List<DetectorFinding> findings) {
    return new CodemodFileScanningResult() {
      @Override
      public List<CodemodChange> changes() {
        return changes;
      }

      @Override
      public List<DetectorFinding> findings() {
        return findings;
      }
    };
  }

  /** Creates an empty instance of {@link CodemodFileScanningResult}. */
  static CodemodFileScanningResult none() {
    return from(List.of(), List.of());
  }

  /** Creates an instance of {@link CodemodFileScanningResult} with only changes. */
  static CodemodFileScanningResult withOnlyChanges(final List<CodemodChange> changes) {
    return from(changes, List.of());
  }

  /** Returns the changes that were made to the file. */
  List<CodemodChange> changes();

  /** Returns the results of the findings that were hoping to be addressed. */
  List<DetectorFinding> findings();
}
