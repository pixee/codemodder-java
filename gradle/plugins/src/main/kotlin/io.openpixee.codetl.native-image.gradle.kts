plugins {
    id("io.openpixee.codetl.base")
    application
    id("org.graalvm.buildtools.native")
    id("io.openpixee.codetl.integration-test")
}

graalvmNative {
    // Despite documentation to the contrary (see https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html#extra-test-suites),
    // GraalVM's test support does not work with our integration test suite. It
    // fails to build the "testlist" file that is required for the
    // nativeIntegrationTest task to succeed.
    testSupport.set(false)
}

tasks.assemble {
    dependsOn(tasks.nativeCompile)
}
