package io.codemodder;

import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFPackageAction;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** A model of a codemod's updating of packages. */
public interface CodemodPackageUpdateResult {

  /** A structured description of what we were able to do. */
  List<CodeTFPackageAction> packageActions();

  /** The changes that were made to the manifest file. */
  List<CodeTFChangesetEntry> manifestChanges();

  /** The set of files that we attempted to update, but failed. */
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
