plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Plugin for providing Maven dependency management functions to codemods."

dependencies {

    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.hamcrest.all)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)

    implementation(libs.commons.lang3)
    implementation(libs.kotlinStdlibJdk8)
    implementation(libs.dom4j)
    implementation(libs.jaxen)
    implementation(libs.xerces.impl)
    implementation(libs.xmlunit.core)
    implementation(testlibs.xmlunit.assertj3)
    implementation(libs.java.semver)
    implementation(libs.juniversalchardet)
    implementation(libs.java.security.toolkit)
    implementation(libs.diff.match.patch)
    implementation(libs.slf4j.simple)
    implementation(libs.slf4j.api)
}
