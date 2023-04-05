plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.guice)
    implementation(libs.contrast.sarif)
    implementation(libs.tuples)
    implementation(libs.java.security.toolkit)
    implementation(libs.slf4j.api)
    api(libs.javaparser.core)
    api(libs.javaparser.symbolsolver.core)
    api(libs.javaparser.symbolsolver.logic)
    api(libs.javaparser.symbolsolver.model)

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
