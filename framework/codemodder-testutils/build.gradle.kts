plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Utilities for testing codemods"

dependencies {
    api(project(":codemodder-base"))

    implementation(testlibs.bundles.junit.jupiter)
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.assertj)
    implementation(testlibs.mockito)

    runtimeOnly(testlibs.junit.jupiter.engine)
}
