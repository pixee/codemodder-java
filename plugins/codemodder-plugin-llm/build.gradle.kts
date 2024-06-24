plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.java-test-fixtures")
    id("io.codemodder.maven-publish")
}

description = "Codemod plugin for augmenting transformation with LLM assisted analysis and fixes"

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base")) {
        exclude(group = "com.google.inject", module = "guice")
    }
    implementation("com.google.inject:guice:5.1.0")

    testFixturesApi(testlibs.junit.jupiter.api)
    testFixturesApi(project(":framework:codemodder-testutils"))
    testFixturesImplementation(testlibs.bundles.hamcrest)

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
