package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import io.codemodder.*;
import io.codemodder.providers.sarif.pmd.PmdScan;
import javax.inject.Inject;

/**
 * A codemod for moving the "default" case to last in switch statements. This codemod is not
 * currently in the default set because it could conceivably change behavior when other case
 * statements fall through to it. It should be improved to only move if the previous case does not
 * fall through.
 */
@Codemod(
    id = "pixee:java/move-switch-default-last",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class MoveSwitchDefaultCaseLastCodemod
    extends SarifPluginJavaParserChanger<SwitchEntry> {

  @Inject
  public MoveSwitchDefaultCaseLastCodemod(
      @PmdScan(ruleId = "category/java/bestpractices.xml/DefaultLabelNotLastInSwitchStmt")
          final RuleSarif ruleSarif) {
    super(ruleSarif, SwitchEntry.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SwitchEntry switchEntry,
      final Result result) {
    SwitchStmt switchStmt = (SwitchStmt) switchEntry.getParentNode().get();
    NodeList<SwitchEntry> entries = switchStmt.getEntries();

    if (entries.size() == 1) {
      // don't mess with the default case if it's the only case
      return false;
    }
    entries.remove(switchEntry);
    entries.addLast(switchEntry);
    return true;
  }
}
