package io.codemodder.remediation;

import io.codemodder.codetf.UnfixedFinding;
import java.util.List;

/** The results of a fix candidate search. */
public interface FixCandidateSearchResults<T> {
  /** The findings that for whichwe could not find potential fix locations. */
  List<UnfixedFinding> unfixableFindings();

  /** The potential fix locations. */
  List<FixCandidate<T>> fixCandidates();
}
