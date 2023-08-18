plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Utilities for testing llm-based codemods"

dependencies {
    implementation(project(":framework:codemodder-base"))
    implementation(project(":plugins:codemodder-plugin-llm"))
    implementation(project(":framework:codemodder-testutils"))
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.bundles.junit.jupiter)
}
