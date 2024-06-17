package io.codemodder.providers.sonar;

import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.javaparser.JavaParserChanger;

/** Provides base functionality for making JavaParser-based remediation of Sonar results. */
public abstract class SonarRemediatingJavaParserChanger extends JavaParserChanger
    implements FixOnlyCodeChanger {

  @Override
  public String vendorName() {
    return "Sonar";
  }
}
