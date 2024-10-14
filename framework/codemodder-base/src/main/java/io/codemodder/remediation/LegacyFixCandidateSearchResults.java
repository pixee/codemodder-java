package io.codemodder.remediation;

import io.codemodder.codetf.UnfixedFinding;
import java.util.List;

/** The results of a fix candidate search. */
@Deprecated
public interface LegacyFixCandidateSearchResults<T> {
  /** The findings that for which we could not find potential fix locations. */
  List<UnfixedFinding> unfixableFindings();

  /** The potential fix locations. */
  List<LegacyFixCandidate<T>> fixCandidates();
}
