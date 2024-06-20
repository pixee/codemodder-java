package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

/**
 * Test for {@link UnsafeReflectionRemediationCodemod} happy path.
 *
 * <p>TODO need to add test cases for edge-cases.
 *
 * <ul>
 *   <li>Fully qualified class name {@code java.lang.Class.forName(String)}
 *   <li>static import {@code forName(String)}
 * </ul>
 */
@Metadata(
    codemodType = UnsafeReflectionRemediationCodemod.class,
    testResourceDir = "unsafe-reflection-s2658",
    renameTestFile = "src/main/java/com/acme/reflection/UnsafeReflection.java",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class UnsafeReflectionRemediationCodemodTest implements CodemodTestMixin {}
