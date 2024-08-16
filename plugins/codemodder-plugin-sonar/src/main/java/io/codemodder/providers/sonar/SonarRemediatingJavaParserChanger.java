package io.codemodder.providers.sonar;

import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.javaparser.JavaParserChanger;
import java.nio.file.Path;

/** Provides base functionality for making JavaParser-based remediation of Sonar results. */
public abstract class SonarRemediatingJavaParserChanger extends JavaParserChanger
    implements FixOnlyCodeChanger {

  private final boolean shouldRun;

  protected SonarRemediatingJavaParserChanger(
      final CodemodReporterStrategy reporter, final RuleFinding<?> findings) {
    super(reporter);
    this.shouldRun = findings.hasResults();
  }

  @Override
  public boolean supports(final Path file) {
    return super.supports(file) && this.shouldRun;
  }

  @Override
  public String vendorName() {
    return "Sonar";
  }
}
