package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

/**
 * This test a case where we expect to: 1. Create 3 new constant variables 2. New constants are
 * snake cased since no other constants were found to check constant naming convention. Snake case
 * is being taken as the default constant naming convention. 3. Replace string value with existing
 * constant that represents it 4. Two of the new constants get rid of characters that are not
 * alphanumeric but keep alphanumeric. 5. One of the new constants is using parent node name since
 * the value that it represents wasn't alphanumeric. 6. No collisions are expected.
 */
@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192/case-5",
    renameTestFile = "codegen/src/main/java/software/amazon/awssdk/codegen/docs/WaiterDocs.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest5 implements CodemodTestMixin {}
