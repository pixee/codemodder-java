package io.codemodder.codemods.codeql;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;
import org.junit.jupiter.api.Nested;

/**
 * This test needs to be fixed once this bug is addressed:
 * https://github.com/pixee/codemodder-java/issues/359
 */
final class CodeQLMavenSecureURLCodemodTest {

  @Nested
  @Metadata(
      codemodType = CodeQLMavenSecureURLCodemod.class,
      testResourceDir = "maven-non-https-url/case-1",
      expectingFixesAtLines = {22},
      dependencies = {})
  final class CodeQLMavenSecureURLCodemodTest1 implements RawFileCodemodTest {}

  @Nested
  @Metadata(
      codemodType = CodeQLMavenSecureURLCodemod.class,
      testResourceDir = "maven-non-https-url/case-2",
      expectingFixesAtLines = {22},
      dependencies = {})
  final class CodeQLMavenSecureURLCodemodTest2 implements RawFileCodemodTest {}

  @Nested
  @Metadata(
      codemodType = CodeQLMavenSecureURLCodemod.class,
      testResourceDir = "maven-non-https-url/wonky",
      expectingFixesAtLines = {26},
      dependencies = {})
  final class CodeQLMavenSecureURLCodemodTestWonky implements RawFileCodemodTest {}
}
