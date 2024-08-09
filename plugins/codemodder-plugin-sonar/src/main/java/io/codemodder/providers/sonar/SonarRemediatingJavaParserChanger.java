package io.codemodder.providers.sonar;

import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.IncludesExcludesPattern;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.List;

/** Provides base functionality for making JavaParser-based remediation of Sonar results. */
public abstract class SonarRemediatingJavaParserChanger extends JavaParserChanger
    implements FixOnlyCodeChanger {

  private final boolean shouldRun;
  private final IncludesExcludesPattern includesExcludesPattern;

  protected SonarRemediatingJavaParserChanger(
      final CodemodReporterStrategy reporter, final RuleFinding<?> findings) {
    super(reporter);
    this.shouldRun = findings.hasResults();
    List<String> allPathFindings = findings.getPaths();
    this.includesExcludesPattern = new IncludesExcludesPattern.Default(allPathFindings, List.of());
  }

  @Override
  public IncludesExcludesPattern getIncludesExcludesPattern() {
    return this.includesExcludesPattern;
  }

  @Override
  public String vendorName() {
    return "Sonar";
  }

  @Override
  public boolean shouldRun() {
    return shouldRun;
  }
}
