package io.codemodder.providers.sarif.semgrep.invalid.bindstoincorrect;

import io.codemodder.CodeChanger;
import io.codemodder.Codemod;
import io.codemodder.CodemodChange;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.codetf.CodeTFReference;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

/** An invalid codemod that binds SARIF to a non-SARIF object. */
@Codemod(
    id = "pixee-test:java/incorrect-binding-type",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class BindsToIncorrectObject implements CodeChanger {
  @Inject
  public BindsToIncorrectObject(
      @SemgrepScan(ruleId = "incorrect-binding-type") HashMap<Object, Object> nonSarifObject) {}

  @Override
  public String getSummary() {
    return "summary";
  }

  @Override
  public String getDescription() {
    return "description";
  }

  @Override
  public List<CodeTFReference> getReferences() {
    return List.of();
  }

  @Override
  public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
    return null;
  }
}
