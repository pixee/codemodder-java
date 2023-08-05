package io.codemodder;

import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFPackageAction;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** A */
public interface CodemodPackageUpdateResult {

  List<CodeTFPackageAction> packageActions();

  List<CodeTFChangesetEntry> manifestChanges();

  Set<Path> filesFailedToChange();

  static CodemodPackageUpdateResult from(
      final List<CodeTFPackageAction> packageActions,
      final List<CodeTFChangesetEntry> manifestChanges,
      final Set<Path> filesFailedToChange) {
    return new CodemodPackageUpdateResult() {
      @Override
      public List<CodeTFPackageAction> packageActions() {
        return packageActions;
      }

      @Override
      public List<CodeTFChangesetEntry> manifestChanges() {
        return manifestChanges;
      }

      @Override
      public Set<Path> filesFailedToChange() {
        return filesFailedToChange;
      }
    };
  }
}
