package io.codemodder.examples;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DummyDecorator.class,
    testResourceDir = "dummy-decorator",
    dependencies = {})
final class DummyDecoratorTest implements CodemodTestMixin {}
