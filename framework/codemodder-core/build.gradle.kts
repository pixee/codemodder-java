plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.dom4j)
    api(libs.guice)
    api(libs.contrast.sarif)
    api(libs.java.security.toolkit)
    api(libs.commons.lang3)
    api("io.codemodder:codetf-java:2.0.0")
    api(libs.slf4j.api)
    api(libs.javaparser.core)
    api(libs.javaparser.symbolsolver.core)
    api(libs.javaparser.symbolsolver.logic)
    api(libs.javaparser.symbolsolver.model)
    api(libs.javadiff)

    implementation(libs.tuples)
    implementation(libs.logback.classic)
    implementation(libs.maven.model)
    implementation(libs.picocli)
    implementation(libs.juniversalchardet)

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
