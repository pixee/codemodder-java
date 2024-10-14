package io.codemodder.remediation;

import io.codemodder.DependencyGAV;
import java.util.List;

/**
 * Represents the result of a strategy fix. Either a list of dependencies to be added, or a reason
 * for failure.
 */
public interface SuccessOrReason {

  /** Checks if the result is a success. */
  boolean isSuccess();

  /** Returns the dependencies to be added from a successful fix. */
  List<DependencyGAV> getDependencies();

  /** Returns the reason for failure. Throw an exception if it's not a failure. */
  String getReason();

  static SuccessOrReason success() {
    return new DefaultSuccessOrReason(List.of());
  }

  static SuccessOrReason success(final List<DependencyGAV> dependencies) {
    return new DefaultSuccessOrReason(dependencies);
  }

  static SuccessOrReason reason(final String reason) {
    return new DefaultSuccessOrReason(reason);
  }
}
