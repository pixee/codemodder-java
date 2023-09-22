plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Plugin for providing JPMS module management functions to codemods."

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base"))
    implementation("com.google.inject:guice:5.1.0")

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
