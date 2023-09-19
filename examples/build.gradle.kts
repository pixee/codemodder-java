plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
    `jvm-test-suite`
}

description = "Example codemods"

dependencies {
    implementation(project(":framework:codemodder-base"))
    implementation(project(":plugins:codemodder-plugin-semgrep"))
    implementation(project(":plugins:codemodder-plugin-codeql"))
    implementation(project(":plugins:codemodder-plugin-maven"))
    implementation(project(":plugins:codemodder-plugin-llm"))
    implementation(project(":plugins:codemodder-plugin-aws"))
    implementation(project(":plugins:codemodder-plugin-pmd"))
    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testImplementation(project(":framework:codemodder-testutils"))
    testImplementation(project(":framework:codemodder-testutils-llm"))
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
