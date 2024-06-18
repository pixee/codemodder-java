package io.codemodder.providers.sonar;

import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.javaparser.JavaParserChanger;

/** Provides base functionality for making JavaParser-based remediation of Sonar results. */
public abstract class SonarRemediatingJavaParserChanger extends JavaParserChanger
    implements FixOnlyCodeChanger {

  protected SonarRemediatingJavaParserChanger(final CodemodReporterStrategy reporter) {
    super(reporter);
  }

  @Override
  public String vendorName() {
    return "Sonar";
  }
}
