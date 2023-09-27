package io.codemodder.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Uses JavaParser to change Java source files. */
public abstract class JavaParserChanger implements CodeChanger {

  protected CodemodReporterStrategy reporter;

  public JavaParserChanger() {
    this.reporter = CodemodReporterStrategy.fromClasspath(this.getClass());
  }

  public JavaParserChanger(final CodemodReporterStrategy reporter) {
    this.reporter = reporter;
  }

  /** Called when a Java file, which has already been parsed into a compilation unit, is seen. */
  public abstract List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu);

  /** Return the list of dependencies associated with the "fix" provided by this changer. */
  public List<DependencyGAV> dependenciesRequired() {
    return List.of();
  }

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
