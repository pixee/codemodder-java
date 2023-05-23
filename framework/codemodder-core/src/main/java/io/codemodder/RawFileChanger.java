package io.codemodder;

import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Gives access to raw files for performing arbitrary changes. */
public abstract class RawFileChanger implements CodeChanger {

  protected CodemodReporterStrategy reporter;

  public RawFileChanger() {
    this.reporter = CodemodReporterStrategy.fromClasspath(this.getClass());
  }

  public RawFileChanger(final CodemodReporterStrategy reporter) {
    this.reporter = reporter;
  }

  /**
   * Visit a file. It is up to the subtype to make sure the file is something to be changed and
   * perform all the changing.
   *
   * @return a list of changes that were made to the file
   */
  public abstract List<CodemodChange> visitFile(CodemodInvocationContext context)
      throws IOException;

  @Override
  public String getSummary() {
    return reporter.getSummary();
  }

  @Override
  public String getDescription() {
    return reporter.getDescription();
  }

  @Override
  public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
    return reporter.getChange(filePath, change);
  }

  @Override
  public List<CodeTFReference> getReferences() {
    return reporter.getReferences().stream()
        .map(u -> new CodeTFReference(u, u))
        .collect(Collectors.toList());
  }
}
