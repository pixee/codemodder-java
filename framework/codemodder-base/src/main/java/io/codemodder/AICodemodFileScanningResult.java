package io.codemodder;

import io.codemodder.codetf.CodeTFAiMetadata;
import io.codemodder.codetf.UnfixedFinding;
import java.util.List;

record AICodemodFileScanningResult(
    List<CodemodChange> changes,
    List<UnfixedFinding> unfixedFindings,
    CodeTFAiMetadata codeTFAiMetadata)
    implements CodemodFileScanningResult, AIMetadataProvider {}
