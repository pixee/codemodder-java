package io.codemodder;

import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFResult;
import java.nio.file.Path;

/** A type that plugins can implement to enrich or modify CodeTF results. */
public interface CodeTFProvider {

  /**
   * Called when a result is created. This allows plugins to modify or replace with a new result.
   */
  default CodeTFResult onResultCreated(CodeTFResult result) {
    return result;
  }

  /**
   * Called when a change is created. This allows plugins to modify or replace with a new change.
   */
  default CodeTFChange onChangeCreated(Path path, String codemod, CodeTFChange change) {
    return change;
  }
}
