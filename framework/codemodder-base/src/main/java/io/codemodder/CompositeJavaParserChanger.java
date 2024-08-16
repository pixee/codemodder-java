package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.javaparser.JavaParserChanger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A type that allows composing multiple {@link JavaParserChanger} instances are under the same
 * umbrella.
 *
 * <p>For instance, you might want to have multiple changers, each with their own SARIF collection,
 * all be reported under the same {@link Codemod#id()}.
 */
public abstract class CompositeJavaParserChanger extends JavaParserChanger {

  private final List<JavaParserChanger> changers;

  protected CompositeJavaParserChanger(
      final CodemodReporterStrategy reporterStrategy, final JavaParserChanger... changers) {
    super(reporterStrategy);
    this.changers = Arrays.asList(Objects.requireNonNull(changers));
  }

  protected CompositeJavaParserChanger(final JavaParserChanger... changers) {
    this.changers = Arrays.asList(Objects.requireNonNull(changers));
  }

  @Override
  public IncludesExcludesPattern getIncludesExcludesPattern() {
    // The first changer will dictate which files this composition accepts
    return changers.get(0).getIncludesExcludesPattern();
  }

  @Override
  public boolean supports(final Path file) {
    return changers.stream().anyMatch(c -> c.supports(file));
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    changers.forEach(
        changer -> {
          CodemodFileScanningResult result = changer.visit(context, cu);
          changes.addAll(result.changes());
          unfixedFindings.addAll(result.unfixedFindings());
        });

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
