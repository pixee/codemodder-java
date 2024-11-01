package io.codemodder.remediation;

import io.codemodder.codetf.UnfixedFinding;
import java.util.List;

/** The results of a fix candidate search. */
public interface FixCandidateSearchResults<T> {

  /** The findings that for which we could not find potential fix locations. */
  List<UnfixedFinding> unfixableFindings();

  /**
   * Issues that were not matched by this searcher
   *
   * @return
   */
  List<T> unmatchedIssues();

  /** The potential fix locations. */
  List<FixCandidate<T>> fixCandidates();
}
