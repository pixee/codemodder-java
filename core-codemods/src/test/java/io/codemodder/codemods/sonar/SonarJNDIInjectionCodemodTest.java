package io.codemodder.codemods.sonar;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarJNDIInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarJNDIInjectionCodemod.class,
      testResourceDir = "sonar-jndi-injection-s2078/normal",
      renameTestFile = "src/main/java/com/mycompany/app/jndi/JNDIVuln.java",
      expectingFixesAtLines = {15},
      dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
  final class ExpectedSinkInLocationTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarJNDIInjectionCodemod.class,
      testResourceDir = "sonar-jndi-injection-s2078/misleading-location",
      renameTestFile = "src/main/java/com/mycompany/app/jndi/FindResource.java",
      expectingFixesAtLines = {18},
      dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
  final class MisleadingSinkInLocationTest implements CodemodTestMixin {}

  /** Just confirms that when there are no changes for a given file, nothing errors. */
  @Nested
  @Metadata(
      codemodType = SonarJNDIInjectionCodemod.class,
      testResourceDir = "sonar-jndi-injection-s2078/unrelated-file",
      renameTestFile = "src/main/java/com/acme/jndi/UnrelatedFile.java",
      dependencies = {})
  final class UnrelatedFileTest implements CodemodTestMixin {}
}
