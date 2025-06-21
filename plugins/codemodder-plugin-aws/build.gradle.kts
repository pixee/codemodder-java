plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Codemod plugin for AWS"

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base")) {
        exclude(group = "com.google.inject", module = "guice")
    }
    implementation("com.google.inject:guice:5.1.0")
    api("software.amazon.awssdk:translate:2.31.68")
    api("software.amazon.awssdk:sso:2.31.68")

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
