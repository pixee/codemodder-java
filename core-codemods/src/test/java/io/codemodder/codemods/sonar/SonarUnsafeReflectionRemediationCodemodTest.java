package io.codemodder.codemods.sonar;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

/**
 * Test for {@link SonarUnsafeReflectionRemediationCodemod}.
 *
 * <ul>
 *   <li>Fully qualified class name {@code java.lang.Class.forName(String)}
 *   <li>static import {@code forName(String)}
 * </ul>
 */
@Metadata(
    codemodType = SonarUnsafeReflectionRemediationCodemod.class,
    testResourceDir = "unsafe-reflection-s2658",
    renameTestFile = "src/main/java/com/acme/reflection/UnsafeReflection.java",
    expectingFixesAtLines = {25},
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SonarUnsafeReflectionRemediationCodemodTest implements CodemodTestMixin {}
