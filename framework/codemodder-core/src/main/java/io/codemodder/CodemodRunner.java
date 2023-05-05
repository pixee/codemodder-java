package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Type responsible for running a codemod on a single file, performing the analysis and file
 * changes.
 */
public interface CodemodRunner {

  /**
   * The {@link java.util.function.Predicate} that determines if this runner supports the given
   * file.
   */
  boolean supports(Path path);

  /**
   * Run the codemod on a single file.
   *
   * @param context the context for the codemod invocation
   * @return an {@link Optional} representing the file "before and after" the codemod
   * @throws IOException if there is an error reading or writing the file
   */
  List<CodemodChange> run(CodemodInvocationContext context) throws IOException;
}
