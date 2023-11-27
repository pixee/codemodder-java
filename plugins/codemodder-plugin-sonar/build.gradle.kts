plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Plugin to enable the use of Sonar in codemods"

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base"))
    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
