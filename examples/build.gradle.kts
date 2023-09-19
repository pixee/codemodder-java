plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
}

description = "Example codemods"

dependencies {
    implementation(project(":framework:codemodder-base"))
    implementation(project(":plugins:codemodder-plugin-semgrep"))
    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testImplementation(project(":framework:codemodder-testutils"))
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
